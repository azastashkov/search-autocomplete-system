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
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrieLoaderServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private TrieLoaderService trieLoaderService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        trieLoaderService = new TrieLoaderService(redisTemplate, jdbcTemplate);
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
    void keepsOldTrieWhenBothRedisAndPostgresFail() {
        // First successful load from Redis
        Set<ZSetOperations.TypedTuple<String>> entries = new LinkedHashSet<>();
        entries.add(ZSetOperations.TypedTuple.of("existing query", 200.0));
        when(zSetOperations.rangeWithScores("autocomplete:frequencies", 0, -1))
                .thenReturn(entries);
        trieLoaderService.reloadTrie();

        // Second load: both fail
        when(zSetOperations.rangeWithScores("autocomplete:frequencies", 0, -1))
                .thenThrow(new RuntimeException("Redis connection failed"));
        when(jdbcTemplate.queryForList("SELECT query, frequency FROM query_frequency"))
                .thenThrow(new RuntimeException("PostgreSQL connection failed"));

        trieLoaderService.reloadTrie();

        Trie currentTrie = trieLoaderService.getTrie();
        assertThat(currentTrie.getSize()).isEqualTo(1);
        assertThat(currentTrie.getTopK("existing", 5)).hasSize(1);
    }

    @Test
    void fallsBackToPostgresWhenRedisIsEmpty() {
        when(zSetOperations.rangeWithScores("autocomplete:frequencies", 0, -1))
                .thenReturn(Collections.emptySet());

        List<Map<String, Object>> pgRows = List.of(
                Map.of("query", "postgres hello", "frequency", 150L),
                Map.of("query", "postgres world", "frequency", 80L)
        );
        when(jdbcTemplate.queryForList("SELECT query, frequency FROM query_frequency"))
                .thenReturn(pgRows);

        trieLoaderService.reloadTrie();

        Trie trie = trieLoaderService.getTrie();
        assertThat(trie.getSize()).isEqualTo(2);
        List<QueryFrequency> results = trie.getTopK("postgres", 5);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).query()).isEqualTo("postgres hello");
    }

    @Test
    void fallsBackToPostgresWhenRedisThrows() {
        when(zSetOperations.rangeWithScores("autocomplete:frequencies", 0, -1))
                .thenThrow(new RuntimeException("Redis connection refused"));

        List<Map<String, Object>> pgRows = List.of(
                Map.of("query", "fallback query", "frequency", 200L)
        );
        when(jdbcTemplate.queryForList("SELECT query, frequency FROM query_frequency"))
                .thenReturn(pgRows);

        trieLoaderService.reloadTrie();

        Trie trie = trieLoaderService.getTrie();
        assertThat(trie.getSize()).isEqualTo(1);
        assertThat(trie.getTopK("fallback", 5).get(0).frequency()).isEqualTo(200);
    }

    @Test
    void keepsExistingTrieWhenBothSourcesReturnEmpty() {
        // First: successful load
        Set<ZSetOperations.TypedTuple<String>> entries = new LinkedHashSet<>();
        entries.add(ZSetOperations.TypedTuple.of("initial query", 100.0));
        when(zSetOperations.rangeWithScores("autocomplete:frequencies", 0, -1))
                .thenReturn(entries);
        trieLoaderService.reloadTrie();
        assertThat(trieLoaderService.getTrie().getSize()).isEqualTo(1);

        // Second: both empty
        when(zSetOperations.rangeWithScores("autocomplete:frequencies", 0, -1))
                .thenReturn(Collections.emptySet());
        when(jdbcTemplate.queryForList("SELECT query, frequency FROM query_frequency"))
                .thenReturn(Collections.emptyList());

        trieLoaderService.reloadTrie();

        assertThat(trieLoaderService.getTrie().getSize()).isEqualTo(1);
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
