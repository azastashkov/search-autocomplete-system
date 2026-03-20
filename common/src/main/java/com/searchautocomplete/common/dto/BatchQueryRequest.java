package com.searchautocomplete.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BatchQueryRequest(
        @NotEmpty(message = "Queries list must not be empty")
        @Size(max = 1000, message = "Batch size must not exceed 1000")
        List<@Valid QueryRequest> queries
) {
}
