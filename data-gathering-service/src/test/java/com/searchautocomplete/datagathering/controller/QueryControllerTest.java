package com.searchautocomplete.datagathering.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.searchautocomplete.common.dto.BatchQueryRequest;
import com.searchautocomplete.common.dto.QueryRequest;
import com.searchautocomplete.datagathering.service.QueryAggregationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueryController.class)
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueryAggregationService queryAggregationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postQuery_returnsAccepted() throws Exception {
        QueryRequest request = new QueryRequest("spring boot");

        mockMvc.perform(post("/api/v1/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(queryAggregationService).addQuery("spring boot");
    }

    @Test
    void postBatchQuery_returnsAccepted() throws Exception {
        BatchQueryRequest request = new BatchQueryRequest(List.of(
                new QueryRequest("java"),
                new QueryRequest("kotlin")
        ));

        mockMvc.perform(post("/api/v1/queries/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(queryAggregationService).addQuery("java");
        verify(queryAggregationService).addQuery("kotlin");
    }

    @Test
    void postQuery_blankQuery_returnsBadRequest() throws Exception {
        QueryRequest request = new QueryRequest("   ");

        mockMvc.perform(post("/api/v1/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postQuery_emptyQuery_returnsBadRequest() throws Exception {
        String json = "{\"query\":\"\"}";

        mockMvc.perform(post("/api/v1/queries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }
}
