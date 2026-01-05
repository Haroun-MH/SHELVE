package com.shelve.bookcatalog.service;

import com.shelve.bookcatalog.dto.BookResponse;
import com.shelve.bookcatalog.dto.CreateBookRequest;
import com.shelve.bookcatalog.dto.PagedResponse;
import com.shelve.bookcatalog.entity.Book;
import com.shelve.bookcatalog.exception.BookNotFoundException;
import com.shelve.bookcatalog.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final ExternalBookService externalBookService;

    public PagedResponse<BookResponse> getAllBooks(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Book> bookPage = bookRepository.findAll(pageable);
        
        return buildPagedResponse(bookPage);
    }

    public BookResponse getBookById(String id) {
        Book book = bookRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + id));
        return mapToResponse(book);
    }

    public List<BookResponse> getBooksByIds(List<String> ids) {
        List<UUID> uuids = ids.stream().map(UUID::fromString).collect(Collectors.toList());
        return bookRepository.findByIdIn(uuids).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public PagedResponse<BookResponse> searchBooks(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.search(query, pageable);
        
        // If local results are insufficient, search external APIs
        if (bookPage.getTotalElements() < size && page == 0) {
            log.info("Local search returned {} results, querying external APIs for: {}", 
                     bookPage.getTotalElements(), query);
            try {
                List<Book> externalBooks = externalBookService.searchExternalApis(query, size * 2);
                
                // Save new books to database and combine results
                List<Book> combined = new ArrayList<>(bookPage.getContent());
                for (Book book : externalBooks) {
                    // Skip if already in local results or database
                    boolean isDuplicate = combined.stream()
                        .anyMatch(b -> isSameBook(b, book));
                    
                    if (!isDuplicate) {
                        // Check if exists in database
                        if (book.getIsbn() != null && bookRepository.existsByIsbn(book.getIsbn())) {
                            continue;
                        }
                        if (bookRepository.existsByTitleAndAuthor(book.getTitle(), book.getAuthor())) {
                            continue;
                        }
                        
                        // Save to database
                        Book saved = bookRepository.save(book);
                        combined.add(saved);
                        
                        if (combined.size() >= size) break;
                    }
                }
                
                // Return combined results
                List<Book> pageContent = combined.stream().limit(size).toList();
                Page<Book> combinedPage = new PageImpl<>(pageContent, pageable, combined.size());
                return buildPagedResponse(combinedPage);
            } catch (Exception e) {
                log.warn("External API search failed: {}", e.getMessage());
                // Fall back to local results only
            }
        }
        
        return buildPagedResponse(bookPage);
    }
    
    private boolean isSameBook(Book a, Book b) {
        // Check by ISBN first
        if (a.getIsbn() != null && b.getIsbn() != null) {
            return a.getIsbn().equals(b.getIsbn());
        }
        // Fall back to title+author comparison
        String titleA = a.getTitle() != null ? a.getTitle().toLowerCase() : "";
        String titleB = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
        String authorA = a.getAuthor() != null ? a.getAuthor().toLowerCase() : "";
        String authorB = b.getAuthor() != null ? b.getAuthor().toLowerCase() : "";
        return titleA.equals(titleB) && authorA.equals(authorB);
    }

    public PagedResponse<BookResponse> getBooksByGenre(String genre, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("averageRating").descending());
        Page<Book> bookPage = bookRepository.findByGenre(genre, pageable);
        return buildPagedResponse(bookPage);
    }

    public List<String> getAllGenres() {
        return bookRepository.findAllGenres();
    }

    public PagedResponse<BookResponse> getTopRatedBooks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.findByOrderByAverageRatingDesc(pageable);
        return buildPagedResponse(bookPage);
    }

    public PagedResponse<BookResponse> getRecentlyAddedBooks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> bookPage = bookRepository.findByOrderByCreatedAtDesc(pageable);
        return buildPagedResponse(bookPage);
    }

    @Transactional
    public BookResponse createBook(CreateBookRequest request) {
        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .description(request.getDescription())
                .coverUrl(request.getCoverUrl())
                .genre(request.getGenre())
                .publishedDate(request.getPublishedDate())
                .pageCount(request.getPageCount())
                .publisher(request.getPublisher())
                .language(request.getLanguage())
                .build();
        
        book = bookRepository.save(book);
        return mapToResponse(book);
    }

    @Transactional
    public void updateRating(String bookId, double newAverageRating, int newRatingsCount) {
        Book book = bookRepository.findById(UUID.fromString(bookId))
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + bookId));
        
        book.setAverageRating(newAverageRating);
        book.setRatingsCount(newRatingsCount);
        bookRepository.save(book);
    }

    private BookResponse mapToResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId().toString())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .description(book.getDescription())
                .coverUrl(book.getCoverUrl())
                .genre(book.getGenre())
                .publishedDate(book.getPublishedDate())
                .pageCount(book.getPageCount())
                .publisher(book.getPublisher())
                .language(book.getLanguage())
                .averageRating(book.getAverageRating())
                .ratingsCount(book.getRatingsCount())
                .build();
    }

    private PagedResponse<BookResponse> buildPagedResponse(Page<Book> bookPage) {
        List<BookResponse> content = bookPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PagedResponse.<BookResponse>builder()
                .content(content)
                .page(bookPage.getNumber())
                .size(bookPage.getSize())
                .totalElements(bookPage.getTotalElements())
                .totalPages(bookPage.getTotalPages())
                .last(bookPage.isLast())
                .build();
    }
}
