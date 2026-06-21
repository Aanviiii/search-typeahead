package com.typeahead.trie;

import java.util.HashMap;
import java.util.Map;

// (1) One node in the Trie tree
// Each node represents ONE character in a query string
public class TrieNode {

    // (2) Children map: character → next TrieNode
    // HashMap chosen over array[26] because queries contain
    // spaces, digits, special chars — not just a-z
    Map<Character, TrieNode> children;

    // (3) Is this node the end of a complete query word?
    // "iphone" → the node at 'e' has isEndOfWord = true
    boolean isEndOfWord;

    // (4) The complete query string stored at end nodes
    // Avoids reconstructing the string by walking back up the tree
    String word;

    // (5) The trending score at this end node
    // score = 0.7 * historicalCount + 0.3 * recentCount
    double score;

    // (6) Constructor — initialise empty children map
    public TrieNode() {
        this.children = new HashMap<>();
        this.isEndOfWord = false;
        this.word = null;
        this.score = 0.0;
    }
}