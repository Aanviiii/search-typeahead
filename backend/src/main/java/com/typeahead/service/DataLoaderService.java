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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    //
    // OPTIMIZED FOR BULK/FRESH LOAD:
    // No per-row DB existence check (224K individual SELECTs was the
    // bottleneck — took 10+ minutes). Instead, we deduplicate in-memory
    // using a HashSet (O(1) lookup, no DB round trip), then bulk-insert.
    // This is correct because this method is only ever called once at
    // startup against what should be an empty/fresh table — there's
    // nothing to "find existing" against yet. The DB's UNIQUE constraint
    // on the `query` column is the final safety net if this assumption
    // is ever violated.
    public void loadFromCsv(String csvPath) {
        System.out.println("[DataLoader] Loading dataset from: " + csvPath);
        long start = System.currentTimeMillis();

        int loaded = 0;
        int skipped = 0;
        int duplicates = 0;

        Set<String> seenInThisLoad = new HashSet<>();
        List<Query> batch = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            String[] header = reader.readNext(); // (2) Skip header row
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

                    // (3) In-memory duplicate check — O(1), no DB hit
                    if (!seenInThisLoad.add(queryText)) {
                        duplicates++;
                        continue;
                    }

                    // (4) Queue for bulk insert
                    batch.add(new Query(queryText, count));
                    loaded++;

                    // (5) Insert into Trie immediately — independent of DB batching
                    trieService.insert(queryText,
                            new Query(queryText, count)
                                    .getTrendingScore(historicalWeight, recentWeight));

                    // (6) Flush to DB every 2000 records to bound memory
                    // and give periodic progress feedback
                    if (batch.size() >= 2000) {
                        queryRepository.saveAll(batch);
                        batch.clear();
                        if (loaded % 20000 < 2000) {
                            System.out.println("[DataLoader] Progress: "
                                    + loaded + " rows loaded so far...");
                        }
                    }

                } catch (NumberFormatException e) {
                    skipped++;
                }
            }

            // (7) Save any remaining records
            if (!batch.isEmpty()) {
                queryRepository.saveAll(batch);
            }

        } catch (IOException | CsvValidationException e) {
            System.err.println("[DataLoader] Failed to load CSV: "
                    + e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf(
                "[DataLoader] Done: %d loaded, %d skipped, %d duplicates removed in %dms. Trie size: %d%n",
                loaded, skipped, duplicates, elapsed, trieService.size());
    }
}