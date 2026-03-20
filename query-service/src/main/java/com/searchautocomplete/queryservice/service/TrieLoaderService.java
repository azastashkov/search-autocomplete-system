package com.searchautocomplete.queryservice.service;

import com.searchautocomplete.queryservice.trie.Trie;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TrieLoaderService {

    private static final Logger log = LoggerFactory.getLogger(TrieLoaderService.class);
    private static final String REDIS_KEY = "autocomplete:frequencies";
    private static final String PG_QUERY = "SELECT query, frequency FROM query_frequency";

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong lastReloadDurationMs = new AtomicLong(0);
    private volatile String lastReloadSource = "none";

    private volatile Trie trie = new Trie();

    public TrieLoaderService(StringRedisTemplate redisTemplate, JdbcTemplate jdbcTemplate) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        reloadTrie();
    }

    @Scheduled(fixedDelay = 60000)
    public void reloadTrie() {
        long startTime = System.currentTimeMillis();
        try {
            Trie newTrie = loadFromRedis();
            if (newTrie != null) {
                this.trie = newTrie;
                lastReloadSource = "redis";
                return;
            }

            newTrie = loadFromPostgres();
            if (newTrie != null) {
                this.trie = newTrie;
                lastReloadSource = "postgres";
                return;
            }

            log.warn("No entries found in Redis or PostgreSQL. Keeping existing trie.");
            lastReloadSource = "empty";
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            lastReloadDurationMs.set(duration);
            log.debug("Trie reload took {} ms (source: {}).", duration, lastReloadSource);
        }
    }

    private Trie loadFromRedis() {
        try {
            Set<ZSetOperations.TypedTuple<String>> entries =
                    redisTemplate.opsForZSet().rangeWithScores(REDIS_KEY, 0, -1);
            if (entries == null || entries.isEmpty()) {
                log.info("Redis returned empty for key '{}'. Trying PostgreSQL fallback.", REDIS_KEY);
                return null;
            }
            Trie newTrie = new Trie();
            for (ZSetOperations.TypedTuple<String> entry : entries) {
                String query = entry.getValue();
                Double score = entry.getScore();
                if (query != null && score != null) {
                    newTrie.insert(query, score.longValue());
                }
            }
            log.info("Trie reloaded from Redis with {} entries.", newTrie.getSize());
            return newTrie;
        } catch (Exception e) {
            log.warn("Redis unavailable: {}. Trying PostgreSQL fallback.", e.getMessage());
            return null;
        }
    }

    private Trie loadFromPostgres() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(PG_QUERY);
            if (rows.isEmpty()) {
                log.info("PostgreSQL returned no entries.");
                return null;
            }
            Trie newTrie = new Trie();
            for (Map<String, Object> row : rows) {
                String query = (String) row.get("query");
                long frequency = ((Number) row.get("frequency")).longValue();
                newTrie.insert(query, frequency);
            }
            log.info("Trie reloaded from PostgreSQL fallback with {} entries.", newTrie.getSize());
            return newTrie;
        } catch (Exception e) {
            log.warn("PostgreSQL fallback also failed: {}", e.getMessage());
            return null;
        }
    }

    public Trie getTrie() {
        return trie;
    }

    public AtomicLong getLastReloadDurationMs() {
        return lastReloadDurationMs;
    }

    public String getLastReloadSource() {
        return lastReloadSource;
    }
}
