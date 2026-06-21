package com.typeahead.batch;

import com.typeahead.cache.DistributedCacheService;
import com.typeahead.model.Query;
import com.typeahead.repository.QueryRepository;
import com.typeahead.trie.TrieService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

// (1) Scheduled component — runs the batch flush on a timer
@Component
public class BatchWriteScheduler {

    private final BatchWriteQueue queue;
    private final QueryRepository queryRepository;
    private final TrieService trieService;
    private final DistributedCacheService cacheService;

    // (2) Constructor injection — Spring wires these automatically
    public BatchWriteScheduler(
            BatchWriteQueue queue,
            QueryRepository queryRepository,
            TrieService trieService,
            DistributedCacheService cacheService) {
        this.queue = queue;
        this.queryRepository = queryRepository;
        this.trieService = trieService;
        this.cacheService = cacheService;
    }

    // ─────────────────────────────────────────────
    // SCHEDULED FLUSH
    // ─────────────────────────────────────────────

    // (3) @Scheduled(fixedDelay = 10000):
    // Wait 10 seconds AFTER the last run completes, then run again
    // fixedDelay (not fixedRate) prevents overlap if flush takes long
    @Scheduled(fixedDelayString = "${app.batch.flush-interval-ms:10000}")
    public void flushBatch() {
        // (4) Drain the queue atomically
        Map<String, Long> batch = queue.drainQueue();

        if (batch.isEmpty()) {
            return; // Nothing to flush
        }

        System.out.println("[Batch] Flushing " + batch.size()
                + " unique queries to DB");

        int written = 0;

        // (5) For each unique query, update or insert in DB
        for (Map.Entry<String, Long> entry : batch.entrySet()) {
            String queryText = entry.getKey();
            long delta = entry.getValue();

            try {
                // (6) Try to increment existing query
                int updated = queryRepository.incrementCount(
                        queryText, delta, LocalDateTime.now());

                if (updated == 0) {
                    // (7) Query doesn't exist yet — insert it
                    Query newQuery = new Query(queryText, delta);
                    queryRepository.save(newQuery);
                }

                // (8) Update Trie score for this query
                // Fetch new score from DB and update Trie
                Optional<Query> updated_query = queryRepository.findByQueryIgnoreCase(queryText);
                updated_query.ifPresent(q -> {
                    double newScore = q.getTrendingScore(0.7, 0.3);
                    trieService.updateScore(queryText, newScore);

                    // (9) Invalidate cache entries that start with any
                    // prefix of this query — forces cache refresh
                    for (int i = 1; i <= queryText.length(); i++) {
                        String prefix = queryText.substring(0, i);
                        cacheService.invalidate(prefix);
                    }
                });

                written++;

            } catch (Exception e) {
                System.err.println("[Batch] Error flushing query '"
                        + queryText + "': " + e.getMessage());
                // (10) Continue with other queries even if one fails
            }
        }

        System.out.println("[Batch] Flush complete: "
                + written + "/" + batch.size() + " queries written");
    }

    // ─────────────────────────────────────────────
    // RECENT COUNT RESET
    // ─────────────────────────────────────────────

    // (11) Every hour, reset recent_count for queries not searched
    // in the last 60 minutes — this prevents permanent recency boost
    @Scheduled(fixedDelay = 3600000) // 1 hour
    public void resetOldRecentCounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        int reset = queryRepository.resetOldRecentCounts(cutoff);
        System.out.println("[Batch] Reset recent_count for "
                + reset + " old queries");
    }
}