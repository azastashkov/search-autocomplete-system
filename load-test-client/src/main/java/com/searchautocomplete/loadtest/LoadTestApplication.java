package com.searchautocomplete.loadtest;

import com.searchautocomplete.loadtest.config.LoadTestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@SpringBootApplication
public class LoadTestApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LoadTestApplication.class);

    private final LoadTestConfig config;
    private final RestTemplate restTemplate;

    public LoadTestApplication(LoadTestConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(LoadTestApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting load test...");
        log.info("Data Gathering URL: {}", config.getDataGatheringUrl());
        log.info("Query Service URL: {}", config.getQueryServiceUrl());
        log.info("Threads: {}, Duration: {}s, Target QPS: {}",
                config.getNumThreads(), config.getDurationSeconds(), config.getTargetQps());

        // Step 1: Seed random words
        List<String> seededWords = seedWords();
        log.info("Seeded {} words", seededWords.size());

        // Step 2: Wait warmup period
        log.info("Waiting 15 seconds for data to propagate...");
        Thread.sleep(15_000);

        // Step 3-6: Run load test
        ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
        AtomicLong totalRequests = new AtomicLong(0);
        AtomicInteger errors = new AtomicInteger(0);

        List<String> prefixes = generatePrefixes(seededWords);
        log.info("Generated {} unique prefixes for testing", prefixes.size());

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (config.getDurationSeconds() * 1000L);

        CountDownLatch latch = new CountDownLatch(config.getNumThreads());

        for (int i = 0; i < config.getNumThreads(); i++) {
            Thread.ofVirtual().name("load-test-", i).start(() -> {
                try {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    while (System.currentTimeMillis() < endTime) {
                        String prefix = prefixes.get(random.nextInt(prefixes.size()));
                        String url = config.getQueryServiceUrl() + "/api/v1/autocomplete?prefix=" + prefix;

                        long reqStart = System.nanoTime();
                        try {
                            restTemplate.getForObject(url, String.class);
                            long latencyMs = (System.nanoTime() - reqStart) / 1_000_000;
                            latencies.add(latencyMs);
                            totalRequests.incrementAndGet();
                        } catch (Exception e) {
                            errors.incrementAndGet();
                            totalRequests.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // Step 7: Print summary
        long actualDuration = System.currentTimeMillis() - startTime;
        printSummary(totalRequests.get(), errors.get(), latencies, actualDuration);

        // Step 8: Exit with code 0
        System.exit(0);
    }

    private List<String> seedWords() {
        List<String> allWords = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            allWords.add(generateRandomWord());
        }

        // Send in batches of 100
        String batchUrl = config.getDataGatheringUrl() + "/api/v1/queries/batch";
        for (int i = 0; i < allWords.size(); i += 100) {
            List<String> batch = allWords.subList(i, Math.min(i + 100, allWords.size()));
            List<Map<String, String>> queries = batch.stream()
                    .map(word -> Map.of("query", word))
                    .collect(Collectors.toList());
            Map<String, Object> request = Map.of("queries", queries);

            try {
                restTemplate.postForObject(batchUrl, request, String.class);
                log.info("Seeded batch {}/{}", (i / 100) + 1, 10);
            } catch (Exception e) {
                log.error("Failed to seed batch: {}", e.getMessage());
            }
        }

        return allWords;
    }

    private String generateRandomWord() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int length = random.nextInt(3, 16); // 3-15 chars
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

    private List<String> generatePrefixes(List<String> words) {
        Set<String> prefixes = new LinkedHashSet<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (String word : words) {
            if (word.length() >= 2) {
                int prefixLen = random.nextInt(2, Math.min(4, word.length() + 1)); // 2 or 3 chars
                prefixes.add(word.substring(0, prefixLen));
            }
        }
        return new ArrayList<>(prefixes);
    }

    private void printSummary(long total, int errorCount, ConcurrentLinkedQueue<Long> latencies, long durationMs) {
        List<Long> sortedLatencies = latencies.stream().sorted().collect(Collectors.toList());

        double durationSec = durationMs / 1000.0;
        double reqPerSec = total / durationSec;
        double errorRate = total > 0 ? (errorCount * 100.0 / total) : 0;

        long p50 = percentile(sortedLatencies, 50);
        long p95 = percentile(sortedLatencies, 95);
        long p99 = percentile(sortedLatencies, 99);

        log.info("========== LOAD TEST RESULTS ==========");
        log.info("Duration:         {:.1f}s", durationSec);
        log.info("Total Requests:   {}", total);
        log.info("Requests/sec:     {:.2f}", reqPerSec);
        log.info("Errors:           {} ({:.2f}%)", errorCount, errorRate);
        log.info("Latency p50:      {}ms", p50);
        log.info("Latency p95:      {}ms", p95);
        log.info("Latency p99:      {}ms", p99);
        log.info("========================================");

        System.out.println();
        System.out.println("========== LOAD TEST RESULTS ==========");
        System.out.printf("Duration:         %.1fs%n", durationSec);
        System.out.printf("Total Requests:   %d%n", total);
        System.out.printf("Requests/sec:     %.2f%n", reqPerSec);
        System.out.printf("Errors:           %d (%.2f%%)%n", errorCount, errorRate);
        System.out.printf("Latency p50:      %dms%n", p50);
        System.out.printf("Latency p95:      %dms%n", p95);
        System.out.printf("Latency p99:      %dms%n", p99);
        System.out.println("========================================");
    }

    private long percentile(List<Long> sortedLatencies, int percentile) {
        if (sortedLatencies.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * sortedLatencies.size()) - 1;
        return sortedLatencies.get(Math.max(0, index));
    }
}
