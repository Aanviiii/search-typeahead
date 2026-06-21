package com.typeahead.trie;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory prefix tree (trie) backing the typeahead suggestions.
 *
 * Each inserted word carries a trending score; {@link #search(String, int)}
 * returns the highest-scoring words that start with a given prefix.
 */
@Service
public class TrieService {

    private static final class Node {
        final Map<Character, Node> children = new HashMap<>();
        boolean isWord;
        String word;
        double score;
    }

    private final Node root = new Node();
    private final AtomicInteger wordCount = new AtomicInteger(0);

    /**
     * Insert a word with its trending score. If the word already exists,
     * its score is updated.
     */
    public void insert(String word, double score) {
        if (word == null || word.isEmpty()) {
            return;
        }
        Node current = root;
        for (int i = 0; i < word.length(); i++) {
            current = current.children.computeIfAbsent(word.charAt(i), c -> new Node());
        }
        if (!current.isWord) {
            current.isWord = true;
            current.word = word;
            wordCount.incrementAndGet();
        }
        current.score = score;
    }

    /**
     * Return up to {@code limit} words that start with {@code prefix},
     * ordered by descending trending score.
     */
    public List<String> search(String prefix, int limit) {
        List<String> results = new ArrayList<>();
        if (prefix == null) {
            return results;
        }
        Node current = root;
        for (int i = 0; i < prefix.length(); i++) {
            current = current.children.get(prefix.charAt(i));
            if (current == null) {
                return results;
            }
        }

        List<Node> matches = new ArrayList<>();
        collect(current, matches);
        matches.sort(Comparator.comparingDouble((Node n) -> n.score).reversed());

        for (Node n : matches) {
            if (results.size() >= limit) {
                break;
            }
            results.add(n.word);
        }
        return results;
    }

    private void collect(Node node, List<Node> out) {
        if (node.isWord) {
            out.add(node);
        }
        for (Node child : node.children.values()) {
            collect(child, out);
        }
    }

    /** Number of distinct words currently stored in the trie. */
    public int size() {
        return wordCount.get();
    }
}
