package com.shelve.review.repository;

import com.shelve.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    
    Page<Review> findByBookId(UUID bookId, Pageable pageable);
    
    List<Review> findByUserId(UUID userId);
    
    Optional<Review> findByUserIdAndBookId(UUID userId, UUID bookId);
    
    boolean existsByUserIdAndBookId(UUID userId, UUID bookId);
}
