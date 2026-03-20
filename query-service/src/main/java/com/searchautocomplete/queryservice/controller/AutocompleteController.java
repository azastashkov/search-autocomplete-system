package com.searchautocomplete.queryservice.controller;

import com.searchautocomplete.common.dto.AutocompleteResponse;
import com.searchautocomplete.queryservice.service.AutocompleteService;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/autocomplete")
@Validated
public class AutocompleteController {

    private static final int DEFAULT_LIMIT = 5;

    private final AutocompleteService autocompleteService;

    public AutocompleteController(AutocompleteService autocompleteService) {
        this.autocompleteService = autocompleteService;
    }

    @GetMapping
    public ResponseEntity<AutocompleteResponse> autocomplete(
            @RequestParam @Size(min = 1, max = 50) String prefix) {

        if (prefix == null || prefix.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        AutocompleteResponse response = autocompleteService.getAutocompleteSuggestions(prefix, DEFAULT_LIMIT);
        return ResponseEntity.ok(response);
    }
}
