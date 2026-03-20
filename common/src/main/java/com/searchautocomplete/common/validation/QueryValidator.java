package com.searchautocomplete.common.validation;

public final class QueryValidator {

    private static final int MAX_QUERY_LENGTH = 50;

    private QueryValidator() {
    }

    public static String normalize(String query) {
        if (query == null) {
            return null;
        }
        return query.strip().toLowerCase();
    }

    public static boolean isValid(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String normalized = query.strip();
        return !normalized.isEmpty() && normalized.length() <= MAX_QUERY_LENGTH;
    }
}
