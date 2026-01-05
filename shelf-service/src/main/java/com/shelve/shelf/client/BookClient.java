package com.shelve.shelf.client;

import com.shelve.shelf.dto.BookResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "book-catalog-service", fallback = BookClientFallback.class)
public interface BookClient {
    
    @GetMapping("/api/books/batch")
    @CircuitBreaker(name = "bookCatalogService")
    List<BookResponse> getBooksByIds(@RequestParam List<String> ids);
}
