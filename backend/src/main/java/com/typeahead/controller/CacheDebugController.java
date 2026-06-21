package com.typeahead.controller;

import com.typeahead.cache.DistributedCacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache")
public class CacheDebugController {

    private final DistributedCacheService cacheService;

    public CacheDebugController(DistributedCacheService cacheService) {
        this.cacheService = cacheService;
    }

    // GET /cache/debug?prefix=iph
    @GetMapping("/debug")
    public ResponseEntity<?> debug(
            @RequestParam(value = "prefix", defaultValue = "") String prefix) {
        return ResponseEntity.ok(cacheService.getDebugInfo(prefix));
    }

    // GET /cache/stats — aggregate stats across all nodes
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(cacheService.getAggregateStats());
    }
}