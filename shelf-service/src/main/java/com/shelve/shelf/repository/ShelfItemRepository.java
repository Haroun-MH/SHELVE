package com.shelve.shelf.repository;

import com.shelve.shelf.entity.ShelfItem;
import com.shelve.shelf.entity.ShelfType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShelfItemRepository extends JpaRepository<ShelfItem, UUID> {
    
    List<ShelfItem> findByUserId(UUID userId);
    
    List<ShelfItem> findByUserIdAndShelfType(UUID userId, ShelfType shelfType);
    
    Optional<ShelfItem> findByUserIdAndBookId(UUID userId, UUID bookId);
    
    boolean existsByUserIdAndBookId(UUID userId, UUID bookId);
    
    void deleteByUserIdAndBookId(UUID userId, UUID bookId);
    
    long countByUserIdAndShelfType(UUID userId, ShelfType shelfType);
}
