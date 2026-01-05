package com.shelve.review.repository;

import com.shelve.review.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {
    
    Optional<Rating> findByUserIdAndBookId(UUID userId, UUID bookId);
    
    List<Rating> findByBookId(UUID bookId);
    
    List<Rating> findByUserId(UUID userId);
    
    List<Rating> findByUserIdAndLikedTrue(UUID userId);
    
    @Query("SELECT AVG(r.score) FROM Rating r WHERE r.bookId = :bookId")
    Double getAverageRatingByBookId(@Param("bookId") UUID bookId);
    
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.bookId = :bookId")
    Integer getRatingsCountByBookId(@Param("bookId") UUID bookId);
    
    boolean existsByUserIdAndBookId(UUID userId, UUID bookId);
}
