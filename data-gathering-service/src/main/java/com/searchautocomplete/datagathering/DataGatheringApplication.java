package com.searchautocomplete.datagathering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DataGatheringApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataGatheringApplication.class, args);
    }
}
