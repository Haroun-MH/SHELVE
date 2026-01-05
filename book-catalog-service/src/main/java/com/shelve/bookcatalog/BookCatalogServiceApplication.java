package com.shelve.bookcatalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class BookCatalogServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookCatalogServiceApplication.class, args);
    }
}
