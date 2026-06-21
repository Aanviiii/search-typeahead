package com.typeahead.config;

import com.typeahead.service.DataLoaderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.File;

@Configuration
public class DataLoaderConfig {

    @Value("${app.dataset.path:/app/dataset/queries.csv}")
    private String datasetPath;

    @Value("${app.dataset.sample-path:/app/dataset/sample_queries.csv}")
    private String samplePath;

    // (1) CommandLineRunner runs after Spring context is fully loaded
    // This is the correct place to load data — all beans are ready
    @Bean
    public CommandLineRunner loadData(DataLoaderService dataLoaderService) {
        return args -> {
            // (2) Try full dataset first, fall back to sample
            File full = new File(datasetPath);
            File sample = new File(samplePath);

            if (full.exists()) {
                dataLoaderService.loadFromCsv(datasetPath);
            } else if (sample.exists()) {
                System.out.println("[DataLoader] Full dataset not found, "
                        + "using sample dataset");
                dataLoaderService.loadFromCsv(samplePath);
            } else {
                System.out.println("[DataLoader] WARNING: No dataset found. "
                        + "Trie will be empty until searches are made.");
            }
        };
    }
}