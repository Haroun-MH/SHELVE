package com.shelve.shelf.client;

import com.shelve.shelf.dto.BookResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Fallback implementation for BookClient when circuit breaker is open
 * or book-catalog-service is unavailable.
 */
@Component
@Slf4j
public class BookClientFallback implements BookClient {

    @Override
    public List<BookResponse> getBooksByIds(List<String> ids) {
        log.warn("Circuit breaker fallback: book-catalog-service unavailable. Returning empty list for {} book IDs", 
                ids != null ? ids.size() : 0);
        return Collections.emptyList();
    }
}
