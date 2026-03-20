package com.searchautocomplete.queryservice.trie;

import java.util.HashMap;
import java.util.Map;

public class TrieNode {

    private final Map<Character, TrieNode> children;
    private long frequency;
    private String word;

    public TrieNode() {
        this.children = new HashMap<>();
        this.frequency = 0;
        this.word = null;
    }

    public Map<Character, TrieNode> getChildren() {
        return children;
    }

    public long getFrequency() {
        return frequency;
    }

    public void setFrequency(long frequency) {
        this.frequency = frequency;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public boolean isEndOfWord() {
        return word != null;
    }
}
