package com.shelve.bookcatalog.controller;

import com.shelve.bookcatalog.dto.BookResponse;
import com.shelve.bookcatalog.dto.CreateBookRequest;
import com.shelve.bookcatalog.dto.PagedResponse;
import com.shelve.bookcatalog.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public ResponseEntity<PagedResponse<BookResponse>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(bookService.getAllBooks(page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable String id) {
        return ResponseEntity.ok(bookService.getBookById(id));
    }

    @GetMapping("/batch")
    public ResponseEntity<List<BookResponse>> getBooksByIds(@RequestParam List<String> ids) {
        return ResponseEntity.ok(bookService.getBooksByIds(ids));
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<BookResponse>> searchBooks(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookService.searchBooks(q, page, size));
    }

    @GetMapping("/genre/{genre}")
    public ResponseEntity<PagedResponse<BookResponse>> getBooksByGenre(
            @PathVariable String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookService.getBooksByGenre(genre, page, size));
    }

    @GetMapping("/genres")
    public ResponseEntity<List<String>> getAllGenres() {
        return ResponseEntity.ok(bookService.getAllGenres());
    }

    @GetMapping("/top-rated")
    public ResponseEntity<PagedResponse<BookResponse>> getTopRatedBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookService.getTopRatedBooks(page, size));
    }

    @GetMapping("/recent")
    public ResponseEntity<PagedResponse<BookResponse>> getRecentlyAddedBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookService.getRecentlyAddedBooks(page, size));
    }

    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.createBook(request));
    }
}
