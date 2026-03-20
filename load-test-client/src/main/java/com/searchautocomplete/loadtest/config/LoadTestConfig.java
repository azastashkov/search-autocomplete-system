package com.searchautocomplete.loadtest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LoadTestConfig {

    @Value("${TARGET_QPS:100}")
    private int targetQps;

    @Value("${DURATION_SECONDS:30}")
    private int durationSeconds;

    @Value("${NUM_THREADS:10}")
    private int numThreads;

    @Value("${DATA_GATHERING_URL:http://localhost:8081}")
    private String dataGatheringUrl;

    @Value("${QUERY_SERVICE_URL:http://localhost:8080}")
    private String queryServiceUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public int getTargetQps() {
        return targetQps;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public String getDataGatheringUrl() {
        return dataGatheringUrl;
    }

    public String getQueryServiceUrl() {
        return queryServiceUrl;
    }
}
