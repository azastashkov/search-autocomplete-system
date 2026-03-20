package com.searchautocomplete.datagathering.service;

import com.searchautocomplete.datagathering.repository.QueryFrequencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class QueryAggregationServiceTest {

    @Mock
    private QueryFrequencyRepository repository;

    @Mock
    private RedisPublisherService redisPublisherService;

    private QueryAggregationService service;

    @BeforeEach
    void setUp() {
        service = new QueryAggregationService(repository, redisPublisherService);
    }

    @Test
    void addQuery_normalizesAndBuffers() {
        service.addQuery("  Spring Boot  ");

        // Flush to verify the query was buffered with normalized form
        service.flushBuffer();

        verify(repository).upsertFrequency(eq("spring boot"), eq(1L));
    }

    @Test
    void addQuery_multipleCallsIncrementCount() {
        service.addQuery("java");
        service.addQuery("java");
        service.addQuery("java");

        service.flushBuffer();

        verify(repository).upsertFrequency(eq("java"), eq(3L));
    }

    @Test
    void flushBuffer_drainsBufferAndCallsRepoAndRedis() {
        service.addQuery("spring");
        service.addQuery("redis");

        service.flushBuffer();

        verify(repository).upsertFrequency(eq("spring"), eq(1L));
        verify(repository).upsertFrequency(eq("redis"), eq(1L));
        verify(redisPublisherService).publishFrequencies(
                Map.of("spring", 1L, "redis", 1L)
        );
    }

    @Test
    void flushBuffer_emptyBuffer_doesNothing() {
        service.flushBuffer();

        verifyNoInteractions(repository);
        verifyNoInteractions(redisPublisherService);
    }

    @Test
    void addQuery_invalidQuery_isRejected() {
        service.addQuery("   ");

        service.flushBuffer();

        verify(repository, never()).upsertFrequency(anyString(), anyLong());
        verify(redisPublisherService, never()).publishFrequencies(anyMap());
    }
}
