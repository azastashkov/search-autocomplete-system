package com.searchautocomplete.queryservice.metrics;

import com.searchautocomplete.queryservice.service.TrieLoaderService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrieMetrics {

    public TrieMetrics(MeterRegistry meterRegistry, TrieLoaderService trieLoaderService) {
        meterRegistry.gauge("trie.size", trieLoaderService,
                loader -> loader.getTrie().getSize());

        meterRegistry.gauge("trie.reload.duration.ms", trieLoaderService.getLastReloadDurationMs(),
                Number::doubleValue);

        for (String source : List.of("redis", "postgres", "empty", "none")) {
            meterRegistry.gauge("trie.reload.source", List.of(Tag.of("source", source)),
                    trieLoaderService, loader -> loader.getLastReloadSource().equals(source) ? 1.0 : 0.0);
        }
    }
}
