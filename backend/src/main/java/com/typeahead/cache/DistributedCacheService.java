package com.typeahead.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

// (1) Public API for the distributed cache
// All other services interact through this class only
@Service
public class DistributedCacheService {

    private final ConsistentHashRouter router;
    private final long ttlSeconds;

    public DistributedCacheService(
            ConsistentHashRouter router,
            @Value("${app.cache.ttl-seconds:60}") long ttlSeconds) {
        this.router = router;
        this.ttlSeconds = ttlSeconds;
    }

    // ─────────────────────────────────────────────
    // GET
    // ─────────────────────────────────────────────

    // (2) Try to get cached suggestions for a prefix
    // Returns null if not cached or expired
    public List<String> get(String prefix) {
        CacheNode node = router.getNode(prefix);
        return node.get(prefix);
    }

    // ─────────────────────────────────────────────
    // PUT
    // ─────────────────────────────────────────────

    // (3) Store suggestions in the correct cache node
    public void put(String prefix, List<String> suggestions) {
        CacheNode node = router.getNode(prefix);
        node.put(prefix, suggestions, ttlSeconds);
    }

    // ─────────────────────────────────────────────
    // INVALIDATE
    // ─────────────────────────────────────────────

    // (4) Remove a prefix from its owning cache node
    // Called after batch flush changes a query's count
    public void invalidate(String prefix) {
        CacheNode node = router.getNode(prefix);
        node.invalidate(prefix);
    }

    // ─────────────────────────────────────────────
    // DEBUG
    // ─────────────────────────────────────────────

    // (5) Return debug info for a prefix — used by /cache/debug endpoint
    public CacheDebugInfo getDebugInfo(String prefix) {
        CacheNode node = router.getNode(prefix);
        List<String> cached = node.get(prefix);
        boolean hit = cached != null;

        return new CacheDebugInfo(
                prefix,
                node.getNodeId(),
                hit,
                hit ? node.getTtlRemaining(prefix) : 0,
                router.getKeyHash(prefix),
                node.getHits(),
                node.getMisses(),
                node.getHitRatio());
    }

    // (6) Scheduled cleanup: evict expired entries every 30 seconds
    @Scheduled(fixedDelay = 30000)
    public void evictExpiredEntries() {
        for (CacheNode node : router.getAllNodes()) {
            node.evictExpired();
        }
    }

    // (7) Get aggregate stats across all nodes
    public String getAggregateStats() {
        long totalHits = 0, totalMisses = 0;
        StringBuilder sb = new StringBuilder();

        for (CacheNode node : router.getAllNodes()) {
            totalHits += node.getHits();
            totalMisses += node.getMisses();
            sb.append(String.format(
                    "%s: size=%d hits=%d misses=%d ratio=%.2f%n",
                    node.getNodeId(),
                    node.getSize(),
                    node.getHits(),
                    node.getMisses(),
                    node.getHitRatio()));
        }

        long total = totalHits + totalMisses;
        double overallRatio = total == 0 ? 0.0 : (double) totalHits / total;
        sb.append(String.format(
                "TOTAL: hits=%d misses=%d ratio=%.2f",
                totalHits, totalMisses, overallRatio));
        return sb.toString();
    }

    // (8) Inner record to carry debug info
    public record CacheDebugInfo(
            String prefix,
            String nodeId,
            boolean hit,
            long ttlRemaining,
            long keyHash,
            long nodeHits,
            long nodeMisses,
            double hitRatio) {
    }
}