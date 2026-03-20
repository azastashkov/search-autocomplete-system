package com.searchautocomplete.queryservice.trie;

import com.searchautocomplete.common.dto.QueryFrequency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TrieTest {

    private Trie trie;

    @BeforeEach
    void setUp() {
        trie = new Trie();
    }

    @Test
    void emptyTrieReturnsEmptyList() {
        List<QueryFrequency> results = trie.getTopK("test", 5);
        assertThat(results).isEmpty();
    }

    @Test
    void singleWordInsertionAndRetrieval() {
        trie.insert("hello", 100);

        List<QueryFrequency> results = trie.getTopK("hel", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).query()).isEqualTo("hello");
        assertThat(results.get(0).frequency()).isEqualTo(100);
    }

    @Test
    void multipleWordsWithDifferentFrequenciesTopFiveOrdering() {
        trie.insert("apple", 500);
        trie.insert("application", 300);
        trie.insert("app", 1000);
        trie.insert("appreciate", 200);
        trie.insert("approach", 700);
        trie.insert("approval", 50);
        trie.insert("appetite", 150);

        List<QueryFrequency> results = trie.getTopK("app", 5);

        assertThat(results).hasSize(5);
        assertThat(results.get(0).frequency()).isGreaterThanOrEqualTo(results.get(1).frequency());
        assertThat(results.get(1).frequency()).isGreaterThanOrEqualTo(results.get(2).frequency());
        assertThat(results.get(2).frequency()).isGreaterThanOrEqualTo(results.get(3).frequency());
        assertThat(results.get(3).frequency()).isGreaterThanOrEqualTo(results.get(4).frequency());

        assertThat(results.get(0).query()).isEqualTo("app");
        assertThat(results.get(0).frequency()).isEqualTo(1000);
    }

    @Test
    void prefixThatDoesNotExistReturnsEmpty() {
        trie.insert("hello", 100);
        trie.insert("world", 200);

        List<QueryFrequency> results = trie.getTopK("xyz", 5);
        assertThat(results).isEmpty();
    }

    @Test
    void exactKResultsWhenMoreEntriesExist() {
        trie.insert("cat", 100);
        trie.insert("car", 200);
        trie.insert("card", 300);
        trie.insert("care", 400);
        trie.insert("carry", 500);
        trie.insert("cart", 600);

        List<QueryFrequency> results = trie.getTopK("ca", 3);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).frequency()).isEqualTo(600);
        assertThat(results.get(1).frequency()).isEqualTo(500);
        assertThat(results.get(2).frequency()).isEqualTo(400);
    }

    @Test
    void frequencyOrderingHigherFirst() {
        trie.insert("search", 10);
        trie.insert("select", 50);
        trie.insert("sell", 30);
        trie.insert("send", 20);

        List<QueryFrequency> results = trie.getTopK("se", 4);

        assertThat(results).hasSize(4);
        assertThat(results.get(0).frequency()).isEqualTo(50);
        assertThat(results.get(1).frequency()).isEqualTo(30);
        assertThat(results.get(2).frequency()).isEqualTo(20);
        assertThat(results.get(3).frequency()).isEqualTo(10);
    }

    @Test
    void commonPrefixHandling() {
        trie.insert("app", 1000);
        trie.insert("apple", 500);
        trie.insert("application", 300);

        assertThat(trie.getSize()).isEqualTo(3);

        List<QueryFrequency> results = trie.getTopK("app", 5);
        assertThat(results).hasSize(3);
        assertThat(results.get(0).query()).isEqualTo("app");
        assertThat(results.get(0).frequency()).isEqualTo(1000);
        assertThat(results.get(1).query()).isEqualTo("apple");
        assertThat(results.get(1).frequency()).isEqualTo(500);
        assertThat(results.get(2).query()).isEqualTo("application");
        assertThat(results.get(2).frequency()).isEqualTo(300);

        List<QueryFrequency> appleResults = trie.getTopK("apple", 5);
        assertThat(appleResults).hasSize(1);
        assertThat(appleResults.get(0).query()).isEqualTo("apple");
    }

    @Test
    void getSizeReturnsCorrectCount() {
        assertThat(trie.getSize()).isEqualTo(0);

        trie.insert("a", 1);
        trie.insert("b", 2);
        trie.insert("c", 3);

        assertThat(trie.getSize()).isEqualTo(3);
    }

    @Test
    void insertSameWordUpdatesFrequency() {
        trie.insert("hello", 100);
        trie.insert("hello", 200);

        assertThat(trie.getSize()).isEqualTo(1);

        List<QueryFrequency> results = trie.getTopK("hel", 5);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).frequency()).isEqualTo(200);
    }
}
