package com.typeahead.controller;

import com.typeahead.service.TrendingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/trending")
public class TrendingController {

    private final TrendingService trendingService;

    public TrendingController(TrendingService trendingService) {
        this.trendingService = trendingService;
    }

    // GET /trending
    @GetMapping
    public ResponseEntity<?> trending() {
        return ResponseEntity.ok(Map.of(
                "trending", trendingService.getTopTrending()));
    }
}