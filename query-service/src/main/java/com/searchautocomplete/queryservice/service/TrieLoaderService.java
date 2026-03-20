package com.searchautocomplete.queryservice.service;

import com.searchautocomplete.queryservice.trie.Trie;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TrieLoaderService {

    private static final Logger log = LoggerFactory.getLogger(TrieLoaderService.class);
    private static final String REDIS_KEY = "autocomplete:frequencies";

    private final StringRedisTemplate redisTemplate;
    private final AtomicLong lastReloadDurationMs = new AtomicLong(0);

    private volatile Trie trie = new Trie();

    public TrieLoaderService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        reloadTrie();
    }

    @Scheduled(fixedDelay = 60000)
    public void reloadTrie() {
        long startTime = System.currentTimeMillis();
        try {
            Set<ZSetOperations.TypedTuple<String>> entries =
                    redisTemplate.opsForZSet().rangeWithScores(REDIS_KEY, 0, -1);

            if (entries == null || entries.isEmpty()) {
                log.info("No entries found in Redis key '{}'. Trie will be empty.", REDIS_KEY);
                this.trie = new Trie();
                return;
            }

            Trie newTrie = new Trie();
            for (ZSetOperations.TypedTuple<String> entry : entries) {
                String query = entry.getValue();
                Double score = entry.getScore();
                if (query != null && score != null) {
                    newTrie.insert(query, score.longValue());
                }
            }

            this.trie = newTrie;
            log.info("Trie reloaded successfully with {} entries.", newTrie.getSize());

        } catch (Exception e) {
            log.warn("Failed to reload trie from Redis. Keeping existing trie.", e);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            lastReloadDurationMs.set(duration);
            log.debug("Trie reload took {} ms.", duration);
        }
    }

    public Trie getTrie() {
        return trie;
    }

    public AtomicLong getLastReloadDurationMs() {
        return lastReloadDurationMs;
    }
}
