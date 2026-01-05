package com.shelve.shelf.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "shelf_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "book_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShelfItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "book_id", nullable = false)
    private UUID bookId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShelfType shelfType;
    
    private LocalDateTime startedAt;
    
    private LocalDateTime finishedAt;
    
    @Column(nullable = false)
    private LocalDateTime addedAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
