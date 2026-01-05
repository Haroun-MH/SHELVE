package com.shelve.bookcatalog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "books")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 500)
    private String title;
    
    @Column(nullable = false, length = 500)
    private String author;
    
    @Column(length = 20)
    private String isbn;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(length = 1000)
    private String coverUrl;
    
    @Column(nullable = false, length = 200)
    private String genre;
    
    private LocalDate publishedDate;
    
    private Integer pageCount;
    
    @Column(length = 500)
    private String publisher;
    
    @Column(length = 10)
    private String language;
    
    @Column(nullable = false)
    private Double averageRating;
    
    @Column(nullable = false)
    private Integer ratingsCount;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (averageRating == null) averageRating = 0.0;
        if (ratingsCount == null) ratingsCount = 0;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
