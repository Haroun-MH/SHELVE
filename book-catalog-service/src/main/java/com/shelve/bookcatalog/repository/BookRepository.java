package com.shelve.bookcatalog.repository;

import com.shelve.bookcatalog.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookRepository extends JpaRepository<Book, UUID> {
    
    Page<Book> findByGenre(String genre, Pageable pageable);
    
    Page<Book> findByAuthorContainingIgnoreCase(String author, Pageable pageable);
    
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.genre) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Book> search(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT DISTINCT b.genre FROM Book b ORDER BY b.genre")
    List<String> findAllGenres();
    
    List<Book> findByIdIn(List<UUID> ids);
    
    Page<Book> findByOrderByAverageRatingDesc(Pageable pageable);
    
    Page<Book> findByGenreOrderByAverageRatingDesc(String genre, Pageable pageable);
    
    // For external API import - duplicate checking
    boolean existsByIsbn(String isbn);
    
    Optional<Book> findByIsbn(String isbn);
    
    boolean existsByTitleAndAuthor(String title, String author);
    
    Page<Book> findByOrderByCreatedAtDesc(Pageable pageable);
}
