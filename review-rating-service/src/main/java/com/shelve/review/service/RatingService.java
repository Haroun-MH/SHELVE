package com.shelve.review.service;

import com.shelve.review.dto.*;
import com.shelve.review.entity.Rating;
import com.shelve.review.event.RatingEvent;
import com.shelve.review.exception.DuplicateRatingException;
import com.shelve.review.exception.RatingNotFoundException;
import com.shelve.review.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final RatingEventPublisher eventPublisher;

    @Transactional
    public RatingResponse createRating(String userId, CreateRatingRequest request) {
        UUID userUUID = UUID.fromString(userId);
        UUID bookUUID = UUID.fromString(request.getBookId());

        if (ratingRepository.existsByUserIdAndBookId(userUUID, bookUUID)) {
            throw new DuplicateRatingException("You have already rated this book");
        }

        Rating rating = Rating.builder()
                .userId(userUUID)
                .bookId(bookUUID)
                .score(request.getScore())
                .liked(request.getScore() >= 4)
                .build();

        rating = ratingRepository.save(rating);

        // Publish event for recommendation service
        publishRatingEvent(rating, "CREATED");

        return mapToResponse(rating);
    }

    @Transactional
    public RatingResponse updateRating(String userId, String bookId, Integer newScore) {
        UUID userUUID = UUID.fromString(userId);
        UUID bookUUID = UUID.fromString(bookId);

        Rating rating = ratingRepository.findByUserIdAndBookId(userUUID, bookUUID)
                .orElseThrow(() -> new RatingNotFoundException("Rating not found"));

        rating.setScore(newScore);
        rating.setLiked(newScore >= 4);
        rating = ratingRepository.save(rating);

        // Publish event for recommendation service
        publishRatingEvent(rating, "UPDATED");

        return mapToResponse(rating);
    }

    @Transactional
    public List<RatingResponse> submitInitialLikedBooks(String userId, InitialLikedBooksRequest request) {
        UUID userUUID = UUID.fromString(userId);

        List<Rating> ratings = request.getBookIds().stream()
                .filter(bookId -> !ratingRepository.existsByUserIdAndBookId(userUUID, UUID.fromString(bookId)))
                .map(bookId -> {
                    Rating rating = Rating.builder()
                            .userId(userUUID)
                            .bookId(UUID.fromString(bookId))
                            .score(5) // Initial liked books get max score
                            .liked(true)
                            .build();
                    return ratingRepository.save(rating);
                })
                .collect(Collectors.toList());

        // Publish events for all new ratings
        ratings.forEach(rating -> publishRatingEvent(rating, "CREATED"));

        return ratings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RatingResponse getUserRatingForBook(String userId, String bookId) {
        UUID userUUID = UUID.fromString(userId);
        UUID bookUUID = UUID.fromString(bookId);

        Rating rating = ratingRepository.findByUserIdAndBookId(userUUID, bookUUID)
                .orElseThrow(() -> new RatingNotFoundException("You haven't rated this book"));

        return mapToResponse(rating);
    }

    public List<RatingResponse> getUserRatings(String userId) {
        UUID userUUID = UUID.fromString(userId);
        return ratingRepository.findByUserId(userUUID).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RatingResponse> getBookRatings(String bookId) {
        UUID bookUUID = UUID.fromString(bookId);
        return ratingRepository.findByBookId(bookUUID).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Double getAverageRating(String bookId) {
        return ratingRepository.getAverageRatingByBookId(UUID.fromString(bookId));
    }

    private void publishRatingEvent(Rating rating, String eventType) {
        RatingEvent event = RatingEvent.builder()
                .userId(rating.getUserId().toString())
                .bookId(rating.getBookId().toString())
                .score(rating.getScore())
                .liked(rating.isLiked())
                .eventType(eventType)
                .timestamp(LocalDateTime.now())
                .build();
        eventPublisher.publishRatingCreated(event);
    }

    private RatingResponse mapToResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId().toString())
                .userId(rating.getUserId().toString())
                .bookId(rating.getBookId().toString())
                .score(rating.getScore())
                .liked(rating.isLiked())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}
