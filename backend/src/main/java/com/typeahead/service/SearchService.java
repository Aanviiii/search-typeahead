package com.typeahead.service;

import com.typeahead.batch.BatchWriteQueue;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private final BatchWriteQueue batchWriteQueue;

    public SearchService(BatchWriteQueue batchWriteQueue) {
        this.batchWriteQueue = batchWriteQueue;
    }

    // (1) Handle a search submission
    // Does NOT write to DB directly — enqueues for batch processing
    public void recordSearch(String query) {
        if (query == null || query.isBlank())
            return;
        // (2) Add to in-memory queue — returns immediately
        batchWriteQueue.enqueue(query.toLowerCase().trim());
    }
}