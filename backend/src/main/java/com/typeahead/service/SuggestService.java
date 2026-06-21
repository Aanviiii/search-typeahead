package com.typeahead.service;

import com.typeahead.cache.DistributedCacheService;
import com.typeahead.trie.TrieService;
import org.springframework.stereotype.Service;

import java.util.List;

// (1) Orchestrates the suggest flow: Cache → Trie → Cache store
@Service
public class SuggestService {

    private final DistributedCacheService cacheService;
    private final TrieService trieService;

    public SuggestService(
            DistributedCacheService cacheService,
            TrieService trieService) {
        this.cacheService = cacheService;
        this.trieService = trieService;
    }

    // (2) Main suggest method — called by SuggestController
    public SuggestResult suggest(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return new SuggestResult(List.of(), false, "");
        }

        String normalised = prefix.toLowerCase().trim();

        // (3) Step 1: Check distributed cache
        List<String> cached = cacheService.get(normalised);

        if (cached != null) {
            // (4) Cache HIT — return immediately, no Trie lookup needed
            return new SuggestResult(cached, true, normalised);
        }

        // (5) Cache MISS — query the Trie
        List<String> suggestions = trieService.search(normalised);

        // (6) Store result in cache for future requests
        // Even empty results are cached to avoid repeated Trie lookups
        cacheService.put(normalised, suggestions);

        return new SuggestResult(suggestions, false, normalised);
    }

    // (7) Result record carrying suggestions + cache hit flag
    public record SuggestResult(
            List<String> suggestions,
            boolean cacheHit,
            String normalisedPrefix) {
    }
}