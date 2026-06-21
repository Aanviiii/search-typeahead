package com.typeahead.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

// (1) Lombok: generates getters, setters, equals, hashCode, toString
@Data
// (2) Lombok: generates no-arg constructor (required by JPA)
@NoArgsConstructor
// (3) Lombok: generates all-arg constructor for convenience
@AllArgsConstructor
// (4) Marks this class as a JPA entity — maps to a DB table
@Entity
// (5) Maps to the "queries" table in SQLite
@Table(name = "queries", indexes = {
        // (6) Index on query column for fast prefix lookups
        @Index(name = "idx_query", columnList = "query")
})
public class Query {

    // (7) Primary key. Uses a sequence-style generator (table-backed on
    // SQLite) instead of IDENTITY: the SQLite JDBC driver doesn't support
    // the getGeneratedKeys path that IDENTITY requires, and block allocation
    // keeps the bulk dataset load fast.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "query_seq")
    @SequenceGenerator(name = "query_seq", sequenceName = "query_seq", allocationSize = 500)
    private Long id;

    // (8) The search query text, must be unique, cannot be null
    @Column(name = "query", unique = true, nullable = false)
    private String query;

    // (9) All-time total search count
    @Column(name = "historical_count")
    private Long historicalCount = 0L;

    // (10) Searches in recent time window (last 1 hour, reset by batch writer)
    @Column(name = "recent_count")
    private Long recentCount = 0L;

    // (11) When this query was last searched — used for recency calculation
    @Column(name = "last_searched_at")
    private LocalDateTime lastSearchedAt;

    // (12) Convenience constructor for creating new queries from dataset
    public Query(String query, Long historicalCount) {
        this.query = query;
        this.historicalCount = historicalCount;
        this.recentCount = 0L;
        this.lastSearchedAt = LocalDateTime.now();
    }

    // (13) Compute trending score — used by TrendingService
    public double getTrendingScore(double historicalWeight, double recentWeight) {
        return (historicalWeight * historicalCount)
                + (recentWeight * recentCount);
    }
}