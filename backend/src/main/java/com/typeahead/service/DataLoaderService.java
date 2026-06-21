package com.typeahead.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.typeahead.model.Query;
import com.typeahead.repository.QueryRepository;
import com.typeahead.trie.TrieService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DataLoaderService {

    private final QueryRepository queryRepository;
    private final TrieService trieService;

    @Value("${app.trending.historical-weight:0.7}")
    private double historicalWeight;

    @Value("${app.trending.recent-weight:0.3}")
    private double recentWeight;

    public DataLoaderService(
            QueryRepository queryRepository,
            TrieService trieService) {
        this.queryRepository = queryRepository;
        this.trieService = trieService;
    }

    // (1) Load dataset from CSV file
    // Called once at startup by DataLoaderConfig
    public void loadFromCsv(String csvPath) {
        System.out.println("[DataLoader] Loading dataset from: " + csvPath);
        long start = System.currentTimeMillis();

        int loaded = 0;
        int skipped = 0;
        List<Query> batch = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            String[] line;

            while ((line = reader.readNext()) != null) {
                try {
                    if (line.length < 2) {
                        skipped++;
                        continue;
                    }

                    String queryText = line[0].trim().toLowerCase();
                    long count = Long.parseLong(line[1].trim());

                    if (queryText.isBlank() || count < 0) {
                        skipped++;
                        continue;
                    }

                    // (3) Check if query already exists in DB
                    Optional<Query> existing = queryRepository.findByQueryIgnoreCase(queryText);

                    if (existing.isPresent()) {
                        // (4) Update Trie with existing score
                        Query q = existing.get();
                        trieService.insert(queryText,
                                q.getTrendingScore(historicalWeight, recentWeight));
                    } else {
                        // (5) Add to batch for bulk insert
                        batch.add(new Query(queryText, count));
                    }

                    loaded++;

                    // (6) Batch save every 1000 records
                    if (batch.size() >= 1000) {
                        queryRepository.saveAll(batch);
                        batch.forEach(q -> trieService.insert(
                                q.getQuery(),
                                q.getTrendingScore(historicalWeight, recentWeight)));
                        batch.clear();
                    }

                } catch (NumberFormatException e) {
                    skipped++;
                }
            }

            // (7) Save remaining records
            if (!batch.isEmpty()) {
                queryRepository.saveAll(batch);
                batch.forEach(q -> trieService.insert(
                        q.getQuery(),
                        q.getTrendingScore(historicalWeight, recentWeight)));
            }

        } catch (IOException | CsvValidationException e) {
            System.err.println("[DataLoader] Failed to load CSV: "
                    + e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf(
                "[DataLoader] Done: %d loaded, %d skipped in %dms. Trie size: %d%n",
                loaded, skipped, elapsed, trieService.size());
    }
}