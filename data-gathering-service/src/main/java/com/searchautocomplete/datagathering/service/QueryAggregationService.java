package com.searchautocomplete.datagathering.service;

import com.searchautocomplete.common.validation.QueryValidator;
import com.searchautocomplete.datagathering.repository.QueryFrequencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Service
public class QueryAggregationService {

    private static final Logger log = LoggerFactory.getLogger(QueryAggregationService.class);

    private final ConcurrentHashMap<String, LongAdder> buffer = new ConcurrentHashMap<>();
    private final QueryFrequencyRepository repository;
    private final RedisPublisherService redisPublisherService;

    public QueryAggregationService(QueryFrequencyRepository repository,
                                   RedisPublisherService redisPublisherService) {
        this.repository = repository;
        this.redisPublisherService = redisPublisherService;
    }

    public void addQuery(String query) {
        String normalized = QueryValidator.normalize(query);
        if (!QueryValidator.isValid(normalized)) {
            log.warn("Invalid query rejected: {}", query);
            return;
        }
        buffer.computeIfAbsent(normalized, k -> new LongAdder()).increment();
    }

    @Scheduled(fixedRate = 5000)
    @Transactional
    public void flushBuffer() {
        if (buffer.isEmpty()) {
            return;
        }

        // Snapshot and reset: swap the entire buffer atomically to avoid
        // losing increments that arrive between sumThenReset() and remove().
        Map<String, LongAdder> snapshot = new HashMap<>();
        buffer.forEach((key, adder) -> {
            long value = adder.sumThenReset();
            if (value > 0) {
                LongAdder a = new LongAdder();
                a.add(value);
                snapshot.put(key, a);
            }
        });

        if (snapshot.isEmpty()) {
            return;
        }

        Map<String, Long> drained = new HashMap<>();
        for (Map.Entry<String, LongAdder> entry : snapshot.entrySet()) {
            drained.put(entry.getKey(), entry.getValue().sum());
        }

        for (Map.Entry<String, Long> entry : drained.entrySet()) {
            repository.upsertFrequency(entry.getKey(), entry.getValue());
        }

        try {
            redisPublisherService.publishFrequencies(drained);
        } catch (Exception e) {
            log.error("Failed to publish frequencies to Redis. Data persisted in PostgreSQL.", e);
        }

        log.info("Flushed {} query frequencies to database and Redis", drained.size());
    }
}
