package com.shelve.shelf.client;

import com.shelve.shelf.dto.BookResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "book-catalog-service")
public interface BookClient {
    
    @GetMapping("/api/books/batch")
    List<BookResponse> getBooksByIds(@RequestParam List<String> ids);
}
