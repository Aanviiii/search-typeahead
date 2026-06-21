package com.typeahead.controller;

import com.typeahead.service.SuggestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/suggest")
public class SuggestController {

    private final SuggestService suggestService;

    public SuggestController(SuggestService suggestService) {
        this.suggestService = suggestService;
    }

    // (1) GET /suggest?q=iph
    @GetMapping
    public ResponseEntity<?> suggest(
            @RequestParam(value = "q", defaultValue = "") String prefix) {
        long start = System.currentTimeMillis();

        SuggestService.SuggestResult result = suggestService.suggest(prefix);

        long latencyMs = System.currentTimeMillis() - start;

        // (2) Return suggestions with metadata
        return ResponseEntity.ok(Map.of(
                "suggestions", result.suggestions(),
                "prefix", result.normalisedPrefix(),
                "cacheHit", result.cacheHit(),
                "latencyMs", latencyMs,
                "count", result.suggestions().size()));
    }
}