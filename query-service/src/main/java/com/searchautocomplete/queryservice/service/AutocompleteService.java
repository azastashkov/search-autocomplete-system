package com.searchautocomplete.queryservice.service;

import com.searchautocomplete.common.dto.AutocompleteResponse;
import com.searchautocomplete.common.dto.QueryFrequency;
import com.searchautocomplete.common.validation.QueryValidator;
import com.searchautocomplete.queryservice.trie.Trie;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AutocompleteService {

    private final TrieLoaderService trieLoaderService;

    public AutocompleteService(TrieLoaderService trieLoaderService) {
        this.trieLoaderService = trieLoaderService;
    }

    @Cacheable(value = "autocomplete", key = "#prefix")
    public AutocompleteResponse getAutocompleteSuggestions(String prefix, int limit) {
        String normalized = QueryValidator.normalize(prefix);

        if (normalized == null || normalized.isEmpty()) {
            return new AutocompleteResponse(prefix, Collections.emptyList());
        }

        Trie trie = trieLoaderService.getTrie();
        List<QueryFrequency> topK = trie.getTopK(normalized, limit);

        List<AutocompleteResponse.Suggestion> suggestions = topK.stream()
                .map(qf -> new AutocompleteResponse.Suggestion(qf.query(), qf.frequency()))
                .toList();

        return new AutocompleteResponse(normalized, suggestions);
    }
}
