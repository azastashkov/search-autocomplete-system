package com.searchautocomplete.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QueryRequest(
        @NotBlank(message = "Query must not be blank")
        @Size(min = 1, max = 50, message = "Query must be between 1 and 50 characters")
        String query
) {
}
