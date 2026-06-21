package com.typeahead.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

// (1) Builds and manages the consistent hash ring
@Component
public class ConsistentHashRouter {

    // (2) The sorted ring: hash position → CacheNode
    // TreeMap keeps keys sorted — critical for clockwise lookup
    private final TreeMap<Long, CacheNode> ring;

    // (3) Physical nodes list
    private final List<CacheNode> physicalNodes;

    // (4) How many virtual nodes per physical node
    private final int virtualNodes;

    // (5) Constructor — builds the ring at startup
    public ConsistentHashRouter(
            @Value("${app.cache.nodes:3}") int nodeCount,
            @Value("${app.cache.virtual-nodes:150}") int virtualNodes) {
        this.ring = new TreeMap<>();
        this.physicalNodes = new ArrayList<>();
        this.virtualNodes = virtualNodes;

        // (6) Create physical nodes and add to ring
        for (int i = 0; i < nodeCount; i++) {
            CacheNode node = new CacheNode("Node-" + i);
            physicalNodes.add(node);
            addNodeToRing(node);
        }

        System.out.println("[Cache] Consistent hash ring built:");
        System.out.println("  Physical nodes: " + nodeCount);
        System.out.println("  Virtual nodes per physical: " + virtualNodes);
        System.out.println("  Total ring positions: " + ring.size());
    }

    // ─────────────────────────────────────────────
    // RING OPERATIONS
    // ─────────────────────────────────────────────

    // (7) Add a node to the ring with its virtual copies
    private void addNodeToRing(CacheNode node) {
        for (int v = 0; v < virtualNodes; v++) {
            // (8) Create unique key for each virtual node
            // Format: "NodeId-VirtualIndex"
            String virtualKey = node.getNodeId() + "-" + v;
            long hash = hash(virtualKey);
            ring.put(hash, node);
        }
    }

    // ─────────────────────────────────────────────
    // ROUTE
    // ─────────────────────────────────────────────

    // (9) Find which CacheNode owns this prefix key
    public CacheNode getNode(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("Hash ring is empty");
        }

        // (10) Hash the key to get its position on the ring
        long keyHash = hash(key);

        // (11) Find the first entry with hash >= keyHash
        // This is "walking clockwise" on the ring
        Map.Entry<Long, CacheNode> entry = ring.ceilingEntry(keyHash);

        // (12) If no entry found (key hash is beyond the last node),
        // wrap around to the first node (circular ring behaviour)
        if (entry == null) {
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    // ─────────────────────────────────────────────
    // HASH FUNCTION
    // ─────────────────────────────────────────────

    // (13) MD5 hash → 32-bit long position on ring
    // MD5 chosen for uniform distribution across the ring
    private long hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());

            // (14) Take first 4 bytes of MD5 and interpret as unsigned long
            // This gives a position in range [0, 2^32)
            long hash = 0;
            for (int i = 0; i < 4; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }
            return hash & 0xFFFFFFFFL; // Ensure positive (unsigned 32-bit)

        } catch (NoSuchAlgorithmException e) {
            // MD5 is always available in Java — this never throws
            throw new RuntimeException("MD5 not available", e);
        }
    }

    // ─────────────────────────────────────────────
    // DEBUG / STATS
    // ─────────────────────────────────────────────

    // (15) Return all physical nodes — used for stats endpoint
    public List<CacheNode> getAllNodes() {
        return Collections.unmodifiableList(physicalNodes);
    }

    // (16) Show ring distribution — for viva demonstration
    public Map<String, Integer> getRingDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        for (CacheNode node : physicalNodes) {
            distribution.put(node.getNodeId(), 0);
        }
        for (CacheNode node : ring.values()) {
            distribution.merge(node.getNodeId(), 1, Integer::sum);
        }
        return distribution;
    }

    // (17) Get the hash position of a key — for debug endpoint
    public long getKeyHash(String key) {
        return hash(key);
    }
}