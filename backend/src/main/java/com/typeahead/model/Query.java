package com.typeahead.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "queries", indexes = {
        @Index(name = "idx_query", columnList = "query")
})
public class Query {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "query", unique = true, nullable = false)
    private String query;

    @Column(name = "historical_count")
    private Long historicalCount = 0L;

    @Column(name = "recent_count")
    private Long recentCount = 0L;

    @Column(name = "last_searched_at")
    private LocalDateTime lastSearchedAt;

    public Query(String query, Long historicalCount) {
        this.query = query;
        this.historicalCount = historicalCount;
        this.recentCount = 0L;
        this.lastSearchedAt = LocalDateTime.now();
    }

    public double getTrendingScore(double historicalWeight, double recentWeight) {
        return (historicalWeight * historicalCount)
                + (recentWeight * recentCount);
    }
}