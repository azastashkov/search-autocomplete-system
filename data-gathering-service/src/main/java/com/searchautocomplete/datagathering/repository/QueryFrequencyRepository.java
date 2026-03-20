package com.searchautocomplete.datagathering.repository;

import com.searchautocomplete.datagathering.entity.QueryFrequencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryFrequencyRepository extends JpaRepository<QueryFrequencyEntity, String> {

    @Modifying
    @Query(value = "INSERT INTO query_frequency (query, frequency) VALUES (:query, :frequency) " +
            "ON CONFLICT (query) DO UPDATE SET frequency = query_frequency.frequency + :frequency",
            nativeQuery = true)
    void upsertFrequency(@Param("query") String query, @Param("frequency") long frequency);
}
