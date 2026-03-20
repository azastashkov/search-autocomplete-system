package com.searchautocomplete.queryservice.controller;

import com.searchautocomplete.common.dto.AutocompleteResponse;
import com.searchautocomplete.queryservice.service.AutocompleteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AutocompleteController.class)
class AutocompleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AutocompleteService autocompleteService;

    @Test
    void validPrefixReturns200WithSuggestions() throws Exception {
        AutocompleteResponse response = new AutocompleteResponse("test", List.of(
                new AutocompleteResponse.Suggestion("testing", 100),
                new AutocompleteResponse.Suggestion("test driven", 50)
        ));

        when(autocompleteService.getAutocompleteSuggestions(eq("test"), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/autocomplete").param("prefix", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prefix").value("test"))
                .andExpect(jsonPath("$.suggestions").isArray())
                .andExpect(jsonPath("$.suggestions.length()").value(2))
                .andExpect(jsonPath("$.suggestions[0].query").value("testing"))
                .andExpect(jsonPath("$.suggestions[0].frequency").value(100));
    }

    @Test
    void missingPrefixReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/autocomplete"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyPrefixReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/autocomplete").param("prefix", ""))
                .andExpect(status().isBadRequest());
    }
}
