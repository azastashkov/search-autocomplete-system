package com.searchautocomplete.queryservice.service;

import com.searchautocomplete.common.dto.AutocompleteResponse;
import com.searchautocomplete.queryservice.trie.Trie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutocompleteServiceTest {

    @Mock
    private TrieLoaderService trieLoaderService;

    @InjectMocks
    private AutocompleteService autocompleteService;

    @Test
    void returnsSuggestionsFromTrie() {
        Trie trie = new Trie();
        trie.insert("testing", 100);
        trie.insert("test driven", 50);

        when(trieLoaderService.getTrie()).thenReturn(trie);

        AutocompleteResponse response = autocompleteService.getAutocompleteSuggestions("test", 5);

        assertThat(response.prefix()).isEqualTo("test");
        assertThat(response.suggestions()).hasSize(2);
        assertThat(response.suggestions().get(0).query()).isEqualTo("testing");
        assertThat(response.suggestions().get(0).frequency()).isEqualTo(100);
        assertThat(response.suggestions().get(1).query()).isEqualTo("test driven");
        assertThat(response.suggestions().get(1).frequency()).isEqualTo(50);
    }

    @Test
    void emptyPrefixReturnsEmpty() {
        AutocompleteResponse response = autocompleteService.getAutocompleteSuggestions("", 5);

        assertThat(response.prefix()).isEqualTo("");
        assertThat(response.suggestions()).isEmpty();
    }

    @Test
    void nullPrefixReturnsEmpty() {
        AutocompleteResponse response = autocompleteService.getAutocompleteSuggestions(null, 5);

        assertThat(response.prefix()).isNull();
        assertThat(response.suggestions()).isEmpty();
    }
}
