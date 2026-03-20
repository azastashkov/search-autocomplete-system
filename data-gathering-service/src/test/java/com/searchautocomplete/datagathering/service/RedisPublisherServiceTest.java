package com.searchautocomplete.datagathering.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisPublisherServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RedisPublisherService service;

    @BeforeEach
    void setUp() {
        service = new RedisPublisherService(redisTemplate);
    }

    @Test
    void publishFrequencies_callsIncrementScoreForEachEntry() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        Map<String, Long> frequencies = Map.of(
                "java", 5L,
                "spring", 3L
        );

        service.publishFrequencies(frequencies);

        verify(zSetOperations).incrementScore("autocomplete:frequencies", "java", 5.0);
        verify(zSetOperations).incrementScore("autocomplete:frequencies", "spring", 3.0);
    }

    @Test
    void publishFrequencies_redisFailure_doesNotThrow() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        doThrow(new RuntimeException("Redis connection refused"))
                .when(zSetOperations).incrementScore(anyString(), anyString(), anyDouble());

        Map<String, Long> frequencies = Map.of("java", 1L);

        assertDoesNotThrow(() -> service.publishFrequencies(frequencies));
    }
}
