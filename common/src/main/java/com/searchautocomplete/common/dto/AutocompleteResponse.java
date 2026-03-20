package com.searchautocomplete.common.dto;

import java.util.List;

public record AutocompleteResponse(
        String prefix,
        List<Suggestion> suggestions
) {
    public record Suggestion(String query, long frequency) {
    }
}
