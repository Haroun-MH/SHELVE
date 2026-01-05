package com.shelve.review.controller;

import com.shelve.review.dto.*;
import com.shelve.review.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    public ResponseEntity<RatingResponse> createRating(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateRatingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ratingService.createRating(userId, request));
    }

    @PostMapping("/initial")
    public ResponseEntity<List<RatingResponse>> submitInitialLikedBooks(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody InitialLikedBooksRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ratingService.submitInitialLikedBooks(userId, request));
    }

    @PutMapping("/book/{bookId}")
    public ResponseEntity<RatingResponse> updateRating(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String bookId,
            @RequestParam Integer score) {
        return ResponseEntity.ok(ratingService.updateRating(userId, bookId, score));
    }

    @GetMapping("/book/{bookId}")
    public ResponseEntity<RatingResponse> getUserRatingForBook(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String bookId) {
        return ResponseEntity.ok(ratingService.getUserRatingForBook(userId, bookId));
    }

    @GetMapping("/user")
    public ResponseEntity<List<RatingResponse>> getUserRatings(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ratingService.getUserRatings(userId));
    }

    @GetMapping("/book/{bookId}/all")
    public ResponseEntity<List<RatingResponse>> getBookRatings(@PathVariable String bookId) {
        return ResponseEntity.ok(ratingService.getBookRatings(bookId));
    }

    @GetMapping("/book/{bookId}/average")
    public ResponseEntity<Double> getAverageRating(@PathVariable String bookId) {
        return ResponseEntity.ok(ratingService.getAverageRating(bookId));
    }
}
