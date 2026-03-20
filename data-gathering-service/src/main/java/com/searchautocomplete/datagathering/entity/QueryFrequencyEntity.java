package com.searchautocomplete.datagathering.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "query_frequency")
public class QueryFrequencyEntity {

    @Id
    @Column(name = "query", length = 50, nullable = false)
    private String query;

    @Column(name = "frequency", nullable = false)
    private long frequency;

    public QueryFrequencyEntity() {
    }

    public QueryFrequencyEntity(String query, long frequency) {
        this.query = query;
        this.frequency = frequency;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getFrequency() {
        return frequency;
    }

    public void setFrequency(long frequency) {
        this.frequency = frequency;
    }
}
