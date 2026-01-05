package com.shelve.shelf.controller;

import com.shelve.shelf.dto.MoveBookRequest;
import com.shelve.shelf.dto.ShelfItemResponse;
import com.shelve.shelf.dto.ShelvesResponse;
import com.shelve.shelf.entity.ShelfType;
import com.shelve.shelf.service.ShelfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/shelves")
@RequiredArgsConstructor
public class ShelfController {

    private final ShelfService shelfService;

    @GetMapping
    public ResponseEntity<ShelvesResponse> getUserShelves(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(shelfService.getUserShelves(userId));
    }

    @GetMapping("/{shelfType}")
    public ResponseEntity<List<ShelfItemResponse>> getShelfByType(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable ShelfType shelfType) {
        return ResponseEntity.ok(shelfService.getShelfByType(userId, shelfType));
    }

    @PostMapping("/{shelfType}/books/{bookId}")
    public ResponseEntity<ShelfItemResponse> addBookToShelf(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable ShelfType shelfType,
            @PathVariable String bookId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shelfService.addBookToShelf(userId, bookId, shelfType));
    }

    @PutMapping("/books/{bookId}")
    public ResponseEntity<ShelfItemResponse> moveBook(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String bookId,
            @Valid @RequestBody MoveBookRequest request) {
        return ResponseEntity.ok(shelfService.moveBook(userId, bookId, request.getTargetShelf()));
    }

    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<Void> removeBookFromShelf(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String bookId) {
        shelfService.removeBookFromShelf(userId, bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/books/{bookId}/status")
    public ResponseEntity<ShelfItemResponse> getBookShelfStatus(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String bookId) {
        Optional<ShelfItemResponse> status = shelfService.getBookShelfStatus(userId, bookId);
        return status.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
