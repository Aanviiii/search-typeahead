package com.typeahead.service;

import com.typeahead.model.Query;
import com.typeahead.repository.QueryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrendingService {

    private final QueryRepository queryRepository;
    private final double historicalWeight;
    private final double recentWeight;
    private final int topN;

    public TrendingService(
            QueryRepository queryRepository,
            @Value("${app.trending.historical-weight:0.7}") double historicalWeight,
            @Value("${app.trending.recent-weight:0.3}") double recentWeight,
            @Value("${app.trending.top-n:10}") int topN) {
        this.queryRepository = queryRepository;
        this.historicalWeight = historicalWeight;
        this.recentWeight = recentWeight;
        this.topN = topN;
    }

    // (1) Fetch top trending queries
    public List<TrendingItem> getTopTrending() {
        // (2) Fetch top N from DB ordered by computed score
        List<Query> queries = queryRepository.findTopTrending(
                PageRequest.of(0, topN));

        // (3) Map to response DTO with computed score
        return queries.stream()
                .map(q -> new TrendingItem(
                        q.getQuery(),
                        q.getTrendingScore(historicalWeight, recentWeight),
                        q.getHistoricalCount(),
                        q.getRecentCount()))
                .toList();
    }

    // (4) Response record
    public record TrendingItem(
            String query,
            double score,
            long historicalCount,
            long recentCount) {
    }
}