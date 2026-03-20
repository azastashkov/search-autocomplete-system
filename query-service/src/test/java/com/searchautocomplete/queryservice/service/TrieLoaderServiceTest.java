package com.searchautocomplete.queryservice.service;

import com.searchautocomplete.common.dto.QueryFrequency;
import com.searchautocomplete.queryservice.trie.Trie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrieLoaderServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private TrieLoaderService trieLoaderService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        trieLoaderService = new TrieLoaderService(redisTemplate);
    }

    @Test
    void reloadTrieBuildsTrieFromRedisData() {
        Set<ZSetOperations.TypedTuple<String>> entries = new LinkedHashSet<>();
        entries.add(ZSetOperations.TypedTuple.of("hello world", 100.0));
        entries.add(ZSetOperations.TypedTuple.of("hello there", 50.0));
        entries.add(ZSetOperations.TypedTuple.of("help me", 75.0));

        when(zSetOperations.rangeWithScores("autocomplete:frequencies", 0, -1))
                .thenReturn(entries);

        trieLoaderService.reloadTrie();

        Trie trie = trieLoaderService.getTrie();
        assertThat(trie.getSize()).isEqualTo(3);

        List<QueryFrequency> results = trie.getTopK("hello", 5);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).query()).isEqualTo("hello world");
        assertThat(results.get(0).frequency()).isEqualTo(100);
    }

    @Test
    void redisFailureKeepsOldTrie() {
        // First successful load
        Set<ZSetOperations.TypedTuple<String>> entries = new LinkedHashSet<>();
        entries.add(ZSetOperations.TypedTuple.of("existing query", 200.0));

        when(zSetOperations.rangeWithScores("autocomplete:frequencies", 0, -1))
                .thenReturn(entries);

        trieLoaderService.reloadTrie();
        Trie firstTrie = trieLoaderService.getTrie();
        assertThat(firstTrie.getSize()).isEqualTo(1);

        // Second load fails
        when(zSetOperations.rangeWithScores("autocomplete:frequencies", 0, -1))
                .thenThrow(new RuntimeException("Redis connection failed"));

        trieLoaderService.reloadTrie();

        // Old trie should still be available
        Trie currentTrie = trieLoaderService.getTrie();
        assertThat(currentTrie.getSize()).isEqualTo(1);
        assertThat(currentTrie.getTopK("existing", 5)).hasSize(1);
    }

    @Test
    void reloadDurationIsTracked() {
        Set<ZSetOperations.TypedTuple<String>> entries = new LinkedHashSet<>();
        when(zSetOperations.rangeWithScores("autocomplete:frequencies", 0, -1))
                .thenReturn(entries);

        trieLoaderService.reloadTrie();

        assertThat(trieLoaderService.getLastReloadDurationMs().get()).isGreaterThanOrEqualTo(0);
    }
}
