package com.searchautocomplete.queryservice.trie;

import com.searchautocomplete.common.dto.QueryFrequency;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class TopKCollector {

    private final int k;
    private final PriorityQueue<QueryFrequency> minHeap;

    public TopKCollector(int k) {
        this.k = k;
        this.minHeap = new PriorityQueue<>(Comparator.comparingLong(QueryFrequency::frequency));
    }

    public void add(QueryFrequency entry) {
        if (minHeap.size() < k) {
            minHeap.offer(entry);
        } else if (!minHeap.isEmpty() && entry.frequency() > minHeap.peek().frequency()) {
            minHeap.poll();
            minHeap.offer(entry);
        }
    }

    public List<QueryFrequency> getResults() {
        List<QueryFrequency> results = new ArrayList<>(minHeap);
        results.sort(Comparator.comparingLong(QueryFrequency::frequency).reversed());
        return results;
    }
}
