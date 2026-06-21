package com.typeahead.trie;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

// (1) Spring-managed singleton — one Trie shared across all requests
@Service
public class TrieService {

    // (2) The root node — empty, represents the start of all queries
    private final TrieNode root;

    // (3) How many suggestions to return
    private static final int TOP_N = 10;

    public TrieService() {
        this.root = new TrieNode();
    }

    // ─────────────────────────────────────────────
    // INSERT
    // ─────────────────────────────────────────────

    // (4) Insert a query into the Trie with its score
    // Time complexity: O(L) where L = length of query string
    public synchronized void insert(String query, double score) {
        if (query == null || query.isBlank())
            return;

        // (5) Normalise to lowercase for case-insensitive matching
        String normalised = query.toLowerCase().trim();

        TrieNode current = root;

        // (6) Walk character by character
        for (char ch : normalised.toCharArray()) {
            // (7) If child for this char doesn't exist, create it
            current.children.putIfAbsent(ch, new TrieNode());
            // (8) Move to the child node
            current = current.children.get(ch);
        }

        // (9) Mark the last node as end of word
        current.isEndOfWord = true;
        current.word = normalised;
        // (10) Update score (in case this query is re-inserted with new score)
        current.score = score;
    }

    // ─────────────────────────────────────────────
    // UPDATE SCORE
    // ─────────────────────────────────────────────

    // (11) Update score for an existing word — called after batch flush
    public synchronized void updateScore(String query, double newScore) {
        if (query == null || query.isBlank())
            return;
        String normalised = query.toLowerCase().trim();
        TrieNode node = walkToNode(normalised);
        if (node != null && node.isEndOfWord) {
            node.score = newScore;
        } else {
            // (12) Word doesn't exist yet — insert it
            insert(normalised, newScore);
        }
    }

    // ─────────────────────────────────────────────
    // SEARCH
    // ─────────────────────────────────────────────

    // (13) Return top 10 suggestions for a given prefix
    // Time complexity: O(L + N) where L=prefix length, N=nodes under prefix
    public List<String> search(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }

        String normalised = prefix.toLowerCase().trim();

        // (14) Walk to the node representing the end of the prefix
        TrieNode prefixNode = walkToNode(normalised);

        // (15) Prefix not found in Trie — return empty list
        if (prefixNode == null) {
            return List.of();
        }

        // (16) Min-heap of size TOP_N — smallest score sits at the root,
        // so we can cheaply evict the weakest candidate as better ones arrive
        PriorityQueue<TrieNode> minHeap = new PriorityQueue<>(
                Comparator.comparingDouble(n -> n.score));

        // (17) DFS from the prefix node — collect all words below
        collectWords(prefixNode, minHeap);

        // (18) Drain heap into a list, then sort descending by score
        List<TrieNode> nodes = new ArrayList<>(minHeap);
        nodes.sort((a, b) -> Double.compare(b.score, a.score));

        List<String> suggestions = new ArrayList<>();
        for (TrieNode node : nodes) {
            suggestions.add(node.word);
        }
        return suggestions;
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    // (20) Walk from root to the node at the end of the given string
    // Returns null if path doesn't exist
    private TrieNode walkToNode(String str) {
        TrieNode current = root;
        for (char ch : str.toCharArray()) {
            if (!current.children.containsKey(ch)) {
                return null; // (21) Prefix not in Trie
            }
            current = current.children.get(ch);
        }
        return current;
    }

    // (22) DFS traversal — collect all end-of-word nodes into the min-heap
    private void collectWords(TrieNode node, PriorityQueue<TrieNode> minHeap) {
        // (23) If this node marks the end of a word, consider adding it
        if (node.isEndOfWord) {
            minHeap.offer(node);
            // (24) If heap exceeds TOP_N, remove the lowest-score word
            // This keeps only the TOP_N highest scores in the heap
            if (minHeap.size() > TOP_N) {
                minHeap.poll(); // removes minimum (lowest score)
            }
        }

        // (25) Recurse into all children
        for (TrieNode child : node.children.values()) {
            collectWords(child, minHeap);
        }
    }

    // ─────────────────────────────────────────────
    // DIAGNOSTICS
    // ─────────────────────────────────────────────

    // (26) Get total number of complete words stored in the Trie
    public int size() {
        return countWords(root);
    }

    private int countWords(TrieNode node) {
        int count = node.isEndOfWord ? 1 : 0;
        for (TrieNode child : node.children.values()) {
            count += countWords(child);
        }
        return count;
    }
}