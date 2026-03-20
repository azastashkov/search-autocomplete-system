package com.searchautocomplete.datagathering.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RedisPublisherService {

    private static final Logger log = LoggerFactory.getLogger(RedisPublisherService.class);
    private static final String SORTED_SET_KEY = "autocomplete:frequencies";

    private final StringRedisTemplate redisTemplate;

    public RedisPublisherService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publishFrequencies(Map<String, Long> frequencies) {
        try {
            for (Map.Entry<String, Long> entry : frequencies.entrySet()) {
                redisTemplate.opsForZSet().incrementScore(
                        SORTED_SET_KEY,
                        entry.getKey(),
                        entry.getValue()
                );
            }
            log.debug("Published {} frequency updates to Redis", frequencies.size());
        } catch (Exception e) {
            log.error("Failed to publish frequencies to Redis: {}", e.getMessage(), e);
        }
    }
}
