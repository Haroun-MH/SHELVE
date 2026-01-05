package com.shelve.bookcatalog.controller;

import com.shelve.bookcatalog.entity.Book;
import com.shelve.bookcatalog.service.ExternalBookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for importing books from external APIs (Google Books & Open Library)
 */
@RestController
@RequestMapping("/api/books/import")
@RequiredArgsConstructor
@Slf4j
public class BookImportController {

    private final ExternalBookService externalBookService;

    /**
     * Search Google Books API
     * GET /api/books/import/google/search?q=harry+potter&limit=10
     */
    @GetMapping("/google/search")
    public ResponseEntity<List<Book>> searchGoogleBooks(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        log.info("Searching Google Books: query={}, limit={}", query, limit);
        List<Book> books = externalBookService.searchGoogleBooks(query, limit);
        return ResponseEntity.ok(books);
    }

    /**
     * Search Open Library API
     * GET /api/books/import/openlibrary/search?q=lord+of+the+rings&limit=10
     */
    @GetMapping("/openlibrary/search")
    public ResponseEntity<List<Book>> searchOpenLibrary(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        log.info("Searching Open Library: query={}, limit={}", query, limit);
        List<Book> books = externalBookService.searchOpenLibrary(query, limit);
        return ResponseEntity.ok(books);
    }

    /**
     * Combined search across Google Books and Open Library
     * GET /api/books/import/search?q=harry+potter&limit=20
     */
    @GetMapping("/search")
    public ResponseEntity<List<Book>> searchAllApis(
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        log.info("Searching all external APIs: query={}, limit={}", query, limit);
        List<Book> books = externalBookService.searchExternalApis(query, limit);
        return ResponseEntity.ok(books);
    }

    /**
     * Get book by ISBN from Google Books
     * GET /api/books/import/isbn/9780261103573
     */
    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<Book> getByIsbn(@PathVariable String isbn) {
        log.info("Fetching book by ISBN: {}", isbn);
        return externalBookService.getBookByIsbn(isbn)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Import books by subject/genre
     * POST /api/books/import/subject
     * Body: { "subject": "science fiction", "count": 20 }
     */
    @PostMapping("/subject")
    public ResponseEntity<Map<String, Object>> importBySubject(
            @RequestBody Map<String, Object> request) {
        String subject = (String) request.get("subject");
        int count = request.containsKey("count") ? (Integer) request.get("count") : 20;
        
        log.info("Importing books by subject: {} (count: {})", subject, count);
        int imported = externalBookService.importBooksBySubject(subject, count);
        
        return ResponseEntity.ok(Map.of(
                "subject", subject,
                "requestedCount", count,
                "importedCount", imported
        ));
    }

    /**
     * Import popular books from multiple genres
     * POST /api/books/import/popular
     */
    @PostMapping("/popular")
    public ResponseEntity<Map<String, Object>> importPopularBooks() {
        log.info("Importing popular books from multiple genres");
        int imported = externalBookService.importPopularBooks();
        
        return ResponseEntity.ok(Map.of(
                "message", "Popular books import completed",
                "totalImported", imported
        ));
    }

    /**
     * Bulk import by multiple subjects
     * POST /api/books/import/bulk
     * Body: { "subjects": ["fiction", "science", "history"], "countPerSubject": 15 }
     */
    @PostMapping("/bulk")
    public ResponseEntity<Map<String, Object>> bulkImport(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> subjects = (List<String>) request.get("subjects");
        int countPerSubject = request.containsKey("countPerSubject") 
                ? (Integer) request.get("countPerSubject") : 20;
        
        log.info("Bulk importing {} books each for {} subjects", countPerSubject, subjects.size());
        
        int totalImported = 0;
        for (String subject : subjects) {
            totalImported += externalBookService.importBooksBySubject(subject, countPerSubject);
        }
        
        return ResponseEntity.ok(Map.of(
                "subjectsProcessed", subjects.size(),
                "requestedPerSubject", countPerSubject,
                "totalImported", totalImported
        ));
    }
}
