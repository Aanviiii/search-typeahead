package com.typeahead.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueryRepository extends JpaRepository<com.typeahead.model.Query, Long> {

    List<com.typeahead.model.Query> findTop10ByQueryStartingWithIgnoreCaseOrderByHistoricalCountDesc(String prefix);

    Optional<com.typeahead.model.Query> findByQueryIgnoreCase(String query);

    @Modifying
    @Transactional
    @Query("UPDATE Query q SET " +
            "q.historicalCount = q.historicalCount + :delta, " +
            "q.recentCount = q.recentCount + :delta, " +
            "q.lastSearchedAt = :now " +
            "WHERE q.query = :queryText")
    int incrementCount(@Param("queryText") String queryText,
            @Param("delta") long delta,
            @Param("now") LocalDateTime now);

    @Query("SELECT q FROM Query q ORDER BY (0.7 * q.historicalCount + 0.3 * q.recentCount) DESC")
    List<com.typeahead.model.Query> findTopTrending(org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Query q SET q.recentCount = 0 WHERE q.lastSearchedAt < :cutoff")
    int resetOldRecentCounts(@Param("cutoff") LocalDateTime cutoff);

    long countBy();
}