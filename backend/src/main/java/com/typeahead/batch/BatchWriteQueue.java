package com.typeahead.batch;

import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

// (1) Thread-safe in-memory queue for search count aggregation
@Component
public class BatchWriteQueue {

    // (2) ConcurrentHashMap: thread-safe, no global lock
    // AtomicLong: thread-safe increment without synchronized blocks
    // Key: query string, Value: pending count increment
    private final ConcurrentHashMap<String, AtomicLong> queue;

    // (3) Track total enqueued for performance metrics
    private final AtomicLong totalEnqueued = new AtomicLong(0);
    private final AtomicLong totalFlushed = new AtomicLong(0);
    private long lastFlushTime = System.currentTimeMillis();

    public BatchWriteQueue() {
        this.queue = new ConcurrentHashMap<>();
    }

    // ─────────────────────────────────────────────
    // ENQUEUE
    // ─────────────────────────────────────────────

    // (4) Record one search for a query
    // Thread-safe: multiple request threads can call this simultaneously
    public void enqueue(String query) {
        if (query == null || query.isBlank())
            return;
        String normalised = query.toLowerCase().trim();

        // (5) computeIfAbsent: atomically create entry if missing
        // incrementAndGet: atomically add 1
        queue.computeIfAbsent(normalised, k -> new AtomicLong(0))
                .incrementAndGet();

        totalEnqueued.incrementAndGet();
    }

    // ─────────────────────────────────────────────
    // DRAIN
    // ─────────────────────────────────────────────

    // (6) Atomically drain the queue and return all pending counts
    // Called by BatchWriteScheduler every 10 seconds
    public Map<String, Long> drainQueue() {
        Map<String, Long> snapshot = new HashMap<>();

        // (7) For each entry, remove it and capture its count
        // This is atomic per key — no data lost between enqueue and drain
        queue.forEach((key, counter) -> {
            // (8) getAndSet(0) atomically reads the value and resets to 0
            // New enqueues after this point start fresh for next batch
            long count = counter.getAndSet(0);
            if (count > 0) {
                snapshot.put(key, count);
            }
        });

        // (9) Remove zero-count entries to keep map clean
        queue.entrySet().removeIf(e -> e.getValue().get() == 0);

        if (!snapshot.isEmpty()) {
            totalFlushed.addAndGet(snapshot.values().stream()
                    .mapToLong(Long::longValue).sum());
            lastFlushTime = System.currentTimeMillis();
        }

        return snapshot;
    }

    // ─────────────────────────────────────────────
    // STATS
    // ─────────────────────────────────────────────

    public int getQueueSize() {
        return queue.size();
    }

    public long getTotalEnqueued() {
        return totalEnqueued.get();
    }

    public long getTotalFlushed() {
        return totalFlushed.get();
    }

    public long getLastFlushTime() {
        return lastFlushTime;
    }

    // (10) Write reduction ratio — used in performance report
    // Shows how many DB writes were avoided by batching.
    // Reads existing counters only — does NOT drain the live queue.
    public String getWriteReductionStats() {
        long enqueued = totalEnqueued.get();
        long flushed = totalFlushed.get();

        if (enqueued == 0)
            return "No data yet";

        double reduction = 1.0 - ((double) queue.size() / enqueued);
        return String.format(
                "Total searches: %d | DB writes (flushed so far): %d | Pending unique queries: %d | Reduction: %.1f%%",
                enqueued,
                flushed,
                queue.size(),
                reduction * 100);
    }
}