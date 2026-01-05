package com.shelve.review.service;

import com.shelve.review.client.AuthServiceClient;
import com.shelve.review.client.UserInfo;
import com.shelve.review.dto.CreateReviewRequest;
import com.shelve.review.dto.PagedResponse;
import com.shelve.review.dto.ReviewResponse;
import com.shelve.review.entity.Review;
import com.shelve.review.exception.DuplicateReviewException;
import com.shelve.review.exception.ReviewNotFoundException;
import com.shelve.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final AuthServiceClient authServiceClient;

    @Transactional
    public ReviewResponse createReview(String userId, CreateReviewRequest request) {
        UUID userUUID = UUID.fromString(userId);
        UUID bookUUID = UUID.fromString(request.getBookId());

        if (reviewRepository.existsByUserIdAndBookId(userUUID, bookUUID)) {
            throw new DuplicateReviewException("You have already reviewed this book");
        }

        Review review = Review.builder()
                .userId(userUUID)
                .bookId(bookUUID)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        review = reviewRepository.save(review);
        return mapToResponseWithUsername(review);
    }

    @Transactional
    public ReviewResponse updateReview(String userId, String reviewId, CreateReviewRequest request) {
        UUID userUUID = UUID.fromString(userId);
        
        Review review = reviewRepository.findById(UUID.fromString(reviewId))
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));

        if (!review.getUserId().equals(userUUID)) {
            throw new ReviewNotFoundException("Review not found");
        }

        review.setTitle(request.getTitle());
        review.setContent(request.getContent());
        review = reviewRepository.save(review);

        return mapToResponseWithUsername(review);
    }

    public PagedResponse<ReviewResponse> getBookReviews(String bookId, int page, int size) {
        UUID bookUUID = UUID.fromString(bookId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviewPage = reviewRepository.findByBookId(bookUUID, pageable);

        // Fetch all user IDs and get usernames in batch
        List<String> userIds = reviewPage.getContent().stream()
                .map(r -> r.getUserId().toString())
                .distinct()
                .collect(Collectors.toList());
        
        Map<String, UserInfo> userInfoMap = authServiceClient.getUserInfoBatch(userIds);

        List<ReviewResponse> content = reviewPage.getContent().stream()
                .map(review -> mapToResponseWithUsername(review, userInfoMap))
                .collect(Collectors.toList());

        return PagedResponse.<ReviewResponse>builder()
                .content(content)
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .build();
    }

    public List<ReviewResponse> getUserReviews(String userId) {
        UUID userUUID = UUID.fromString(userId);
        return reviewRepository.findByUserId(userUUID).stream()
                .map(this::mapToResponseWithUsername)
                .collect(Collectors.toList());
    }

    public ReviewResponse getReviewById(String reviewId) {
        Review review = reviewRepository.findById(UUID.fromString(reviewId))
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));
        return mapToResponseWithUsername(review);
    }

    @Transactional
    public void deleteReview(String userId, String reviewId) {
        UUID userUUID = UUID.fromString(userId);
        
        Review review = reviewRepository.findById(UUID.fromString(reviewId))
                .orElseThrow(() -> new ReviewNotFoundException("Review not found"));

        if (!review.getUserId().equals(userUUID)) {
            throw new ReviewNotFoundException("Review not found");
        }

        reviewRepository.delete(review);
    }

    private ReviewResponse mapToResponseWithUsername(Review review) {
        UserInfo userInfo = authServiceClient.getUserInfo(review.getUserId().toString());
        String username = userInfo != null ? userInfo.getName() : "Anonymous";
        
        return ReviewResponse.builder()
                .id(review.getId().toString())
                .userId(review.getUserId().toString())
                .username(username)
                .bookId(review.getBookId().toString())
                .title(review.getTitle())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
    
    private ReviewResponse mapToResponseWithUsername(Review review, Map<String, UserInfo> userInfoMap) {
        UserInfo userInfo = userInfoMap.get(review.getUserId().toString());
        String username = userInfo != null ? userInfo.getName() : "Anonymous";
        
        return ReviewResponse.builder()
                .id(review.getId().toString())
                .userId(review.getUserId().toString())
                .username(username)
                .bookId(review.getBookId().toString())
                .title(review.getTitle())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
