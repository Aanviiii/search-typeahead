package com.typeahead.cache;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// (1) One logical cache node in the distributed cache ring
// In a real system this would be a separate Redis/Memcached instance
// Here it's simulated as an in-process HashMap with TTL
public class CacheNode {

    // (2) Unique identifier for this node (e.g., "Node-0")
    private final String nodeId;

    // (3) Inner class representing one cached entry
    private static class CacheEntry {
        List<String> suggestions; // The cached suggestions list
        Instant expiryTime; // When this entry expires

        CacheEntry(List<String> suggestions, long ttlSeconds) {
            this.suggestions = suggestions;
            // (4) Calculate absolute expiry time
            this.expiryTime = Instant.now().plusSeconds(ttlSeconds);
        }

        // (5) Check if this entry has expired
        boolean isExpired() {
            return Instant.now().isAfter(expiryTime);
        }

        // (6) How many seconds remain before expiry
        long ttlRemaining() {
            long remaining = expiryTime.getEpochSecond()
                    - Instant.now().getEpochSecond();
            return Math.max(0, remaining);
        }
    }

    // (7) The actual storage: prefix → CacheEntry
    private final Map<String, CacheEntry> store;

    // (8) Read-Write lock: multiple readers allowed simultaneously,
    // only one writer at a time, writers block readers
    private final ReentrantReadWriteLock lock;

    // (9) Statistics counters
    private long hits = 0;
    private long misses = 0;
    private long evictions = 0;

    public CacheNode(String nodeId) {
        this.nodeId = nodeId;
        this.store = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    // ─────────────────────────────────────────────
    // GET
    // ─────────────────────────────────────────────

    // (10) Retrieve suggestions for a prefix
    // Returns null on miss or expiry
    public List<String> get(String prefix) {
        // (11) Acquire read lock — allows concurrent reads
        lock.readLock().lock();
        try {
            CacheEntry entry = store.get(prefix);

            if (entry == null) {
                misses++;
                return null; // Cache miss
            }

            if (entry.isExpired()) {
                misses++;
                // (12) Don't delete here (we hold read lock, not write lock)
                // Expired entry will be cleaned on next write or explicit evict
                return null; // Treat expired as miss
            }

            hits++;
            return entry.suggestions; // Cache hit

        } finally {
            // (13) ALWAYS release lock in finally block
            lock.readLock().unlock();
        }
    }

    // ─────────────────────────────────────────────
    // PUT
    // ─────────────────────────────────────────────

    // (14) Store suggestions for a prefix with TTL
    public void put(String prefix, List<String> suggestions, long ttlSeconds) {
        lock.writeLock().lock();
        try {
            store.put(prefix, new CacheEntry(suggestions, ttlSeconds));
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ─────────────────────────────────────────────
    // INVALIDATE
    // ─────────────────────────────────────────────

    // (15) Remove a specific prefix from cache
    // Called when a query's count changes (after batch flush)
    public void invalidate(String prefix) {
        lock.writeLock().lock();
        try {
            if (store.remove(prefix) != null) {
                evictions++;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // (16) Evict all expired entries — called periodically
    public void evictExpired() {
        lock.writeLock().lock();
        try {
            store.entrySet().removeIf(e -> {
                if (e.getValue().isExpired()) {
                    evictions++;
                    return true;
                }
                return false;
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ─────────────────────────────────────────────
    // DEBUG / STATS
    // ─────────────────────────────────────────────

    public String getNodeId() {
        return nodeId;
    }

    public long getHits() {
        return hits;
    }

    public long getMisses() {
        return misses;
    }

    public long getEvictions() {
        return evictions;
    }

    public int getSize() {
        return store.size();
    }

    // (17) Check TTL remaining for a prefix — used by debug endpoint
    public long getTtlRemaining(String prefix) {
        lock.readLock().lock();
        try {
            CacheEntry entry = store.get(prefix);
            if (entry == null || entry.isExpired())
                return 0;
            return entry.ttlRemaining();
        } finally {
            lock.readLock().unlock();
        }
    }

    public double getHitRatio() {
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total;
    }
}