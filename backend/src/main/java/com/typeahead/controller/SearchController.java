package com.typeahead.controller;

import com.typeahead.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    // (1) POST /search with body {"query":"iphone"}
    @PostMapping
    public ResponseEntity<?> search(@RequestBody Map<String, String> body) {
        String query = body.get("query");

        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "query is required"));
        }

        // (2) Record the search asynchronously via batch queue
        searchService.recordSearch(query);

        // (3) Return exact format required by assignment
        return ResponseEntity.ok(Map.of("message", "Searched"));
    }
}