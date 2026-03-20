package com.searchautocomplete.datagathering.controller;

import com.searchautocomplete.common.dto.BatchQueryRequest;
import com.searchautocomplete.common.dto.QueryRequest;
import com.searchautocomplete.datagathering.service.QueryAggregationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/queries")
public class QueryController {

    private final QueryAggregationService queryAggregationService;

    public QueryController(QueryAggregationService queryAggregationService) {
        this.queryAggregationService = queryAggregationService;
    }

    @PostMapping
    public ResponseEntity<Void> postQuery(@Valid @RequestBody QueryRequest request) {
        queryAggregationService.addQuery(request.query());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/batch")
    public ResponseEntity<Void> postBatchQuery(@Valid @RequestBody BatchQueryRequest request) {
        for (QueryRequest queryRequest : request.queries()) {
            queryAggregationService.addQuery(queryRequest.query());
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
