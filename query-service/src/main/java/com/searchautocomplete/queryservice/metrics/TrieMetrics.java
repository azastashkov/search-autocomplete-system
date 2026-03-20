package com.searchautocomplete.queryservice.metrics;

import com.searchautocomplete.queryservice.service.TrieLoaderService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class TrieMetrics {

    public TrieMetrics(MeterRegistry meterRegistry, TrieLoaderService trieLoaderService) {
        meterRegistry.gauge("trie.size", trieLoaderService,
                loader -> loader.getTrie().getSize());

        meterRegistry.gauge("trie.reload.duration.ms", trieLoaderService.getLastReloadDurationMs(),
                Number::doubleValue);
    }
}
