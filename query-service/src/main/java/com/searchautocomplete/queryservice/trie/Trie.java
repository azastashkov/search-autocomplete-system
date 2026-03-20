package com.searchautocomplete.queryservice.trie;

import com.searchautocomplete.common.dto.QueryFrequency;

import java.util.Collections;
import java.util.List;

public class Trie {

    private final TrieNode root;
    private int size;

    public Trie() {
        this.root = new TrieNode();
        this.size = 0;
    }

    public void insert(String word, long frequency) {
        if (word == null || word.isEmpty()) {
            return;
        }

        TrieNode current = root;
        for (char ch : word.toCharArray()) {
            current = current.getChildren().computeIfAbsent(ch, c -> new TrieNode());
        }

        if (!current.isEndOfWord()) {
            size++;
        }
        current.setWord(word);
        current.setFrequency(frequency);
    }

    public List<QueryFrequency> getTopK(String prefix, int k) {
        if (prefix == null || prefix.isEmpty() || k <= 0) {
            return Collections.emptyList();
        }

        TrieNode node = navigateToPrefix(prefix);
        if (node == null) {
            return Collections.emptyList();
        }

        TopKCollector collector = new TopKCollector(k);
        collectTerminalNodes(node, collector);
        return collector.getResults();
    }

    public int getSize() {
        return size;
    }

    private TrieNode navigateToPrefix(String prefix) {
        TrieNode current = root;
        for (char ch : prefix.toCharArray()) {
            current = current.getChildren().get(ch);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private void collectTerminalNodes(TrieNode node, TopKCollector collector) {
        if (node.isEndOfWord()) {
            collector.add(new QueryFrequency(node.getWord(), node.getFrequency()));
        }
        for (TrieNode child : node.getChildren().values()) {
            collectTerminalNodes(child, collector);
        }
    }
}
