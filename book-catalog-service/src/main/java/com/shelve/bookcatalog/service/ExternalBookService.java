package com.shelve.bookcatalog.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.shelve.bookcatalog.entity.Book;
import com.shelve.bookcatalog.repository.BookRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Service for fetching book data from external APIs:
 * - Google Books API (primary)
 * - Open Library API (fallback/supplementary)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalBookService {

    private final BookRepository bookRepository;
    private final WebClient.Builder webClientBuilder;

    @Value("${google.books.api.key:}")
    private String googleBooksApiKey;

    private static final String GOOGLE_BOOKS_API = "https://www.googleapis.com/books/v1/volumes";
    private static final String OPEN_LIBRARY_API = "https://openlibrary.org";
    private static final String OPEN_LIBRARY_COVERS = "https://covers.openlibrary.org";

    /**
     * Search for books using Google Books API
     */
    public List<Book> searchGoogleBooks(String query, int maxResults) {
        log.info("Searching Google Books for: {}", query);
        
        WebClient client = webClientBuilder.baseUrl(GOOGLE_BOOKS_API).build();
        
        String uri = "?q=" + query + "&maxResults=" + maxResults;
        if (googleBooksApiKey != null && !googleBooksApiKey.isEmpty()) {
            uri += "&key=" + googleBooksApiKey;
        }
        
        GoogleBooksResponse response = client.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(GoogleBooksResponse.class)
            .block();
        
        if (response == null || response.getItems() == null) {
            log.warn("No results from Google Books API");
            return List.of();
        }
        
        return response.getItems().stream()
            .map(this::convertGoogleBookToEntity)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    /**
     * Get book details by ISBN from Google Books
     */
    public Optional<Book> getBookByIsbn(String isbn) {
        log.info("Fetching book by ISBN: {}", isbn);
        
        List<Book> results = searchGoogleBooks("isbn:" + isbn, 1);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Search Open Library for books
     */
    public List<Book> searchOpenLibrary(String query, int limit) {
        log.info("Searching Open Library for: {}", query);
        
        WebClient client = webClientBuilder.baseUrl(OPEN_LIBRARY_API).build();
        
        OpenLibrarySearchResponse response = client.get()
            .uri("/search.json?q={query}&limit={limit}", query, limit)
            .retrieve()
            .bodyToMono(OpenLibrarySearchResponse.class)
            .block();
        
        if (response == null || response.getDocs() == null) {
            log.warn("No results from Open Library API");
            return List.of();
        }
        
        return response.getDocs().stream()
            .map(this::convertOpenLibraryToEntity)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    /**
     * Get book cover URL from Open Library by ISBN
     */
    public String getOpenLibraryCoverUrl(String isbn, String size) {
        // Size: S, M, L
        return OPEN_LIBRARY_COVERS + "/b/isbn/" + isbn + "-" + size + ".jpg";
    }

    /**
     * Get book cover URL from Open Library by cover ID
     */
    public String getOpenLibraryCoverUrlById(Long coverId, String size) {
        return OPEN_LIBRARY_COVERS + "/b/id/" + coverId + "-" + size + ".jpg";
    }

    /**
     * Import books from Google Books by subject/category
     */
    public int importBooksBySubject(String subject, int count) {
        log.info("Importing {} books for subject: {}", count, subject);
        
        List<Book> books = searchGoogleBooks("subject:" + subject, count);
        int imported = 0;
        
        for (Book book : books) {
            if (book.getIsbn() != null && !bookRepository.existsByIsbn(book.getIsbn())) {
                bookRepository.save(book);
                imported++;
            } else if (book.getIsbn() == null && !bookRepository.existsByTitleAndAuthor(book.getTitle(), book.getAuthor())) {
                bookRepository.save(book);
                imported++;
            }
        }
        
        log.info("Imported {} new books for subject: {}", imported, subject);
        return imported;
    }

    /**
     * Import popular books from multiple genres
     */
    public int importPopularBooks() {
        String[] subjects = {
            "fiction bestsellers",
            "science fiction classics",
            "fantasy novels",
            "mystery thriller",
            "romance novels",
            "historical fiction",
            "biography",
            "self-help",
            "science",
            "philosophy"
        };
        
        int totalImported = 0;
        for (String subject : subjects) {
            totalImported += importBooksBySubject(subject, 20);
        }
        
        log.info("Total books imported: {}", totalImported);
        return totalImported;
    }

    private Optional<Book> convertGoogleBookToEntity(GoogleBooksItem item) {
        try {
            GoogleVolumeInfo info = item.getVolumeInfo();
            if (info == null || info.getTitle() == null) {
                return Optional.empty();
            }
            
            Book book = new Book();
            book.setTitle(truncate(info.getTitle(), 490));
            book.setAuthor(truncate(info.getAuthors() != null ? String.join(", ", info.getAuthors()) : "Unknown", 490));
            book.setDescription(truncate(info.getDescription(), 4000));
            book.setPageCount(info.getPageCount());
            book.setPublisher(truncate(info.getPublisher(), 490));
            book.setLanguage(truncate(info.getLanguage() != null ? info.getLanguage() : "en", 10));
            
            // Extract ISBN
            if (info.getIndustryIdentifiers() != null) {
                for (IndustryIdentifier id : info.getIndustryIdentifiers()) {
                    if ("ISBN_13".equals(id.getType())) {
                        book.setIsbn(id.getIdentifier());
                        break;
                    } else if ("ISBN_10".equals(id.getType()) && book.getIsbn() == null) {
                        book.setIsbn(id.getIdentifier());
                    }
                }
            }
            
            // Cover image - prefer Google's thumbnail, fallback to Open Library
            if (info.getImageLinks() != null && info.getImageLinks().getThumbnail() != null) {
                // Convert HTTP to HTTPS and get larger image
                String coverUrl = info.getImageLinks().getThumbnail()
                    .replace("http://", "https://")
                    .replace("zoom=1", "zoom=2");
                book.setCoverUrl(coverUrl);
            } else if (book.getIsbn() != null) {
                book.setCoverUrl(getOpenLibraryCoverUrl(book.getIsbn(), "L"));
            }
            
            // Parse published date
            if (info.getPublishedDate() != null) {
                book.setPublishedDate(parsePublishedDate(info.getPublishedDate()));
            }
            
            // Extract genre from categories
            if (info.getCategories() != null && !info.getCategories().isEmpty()) {
                book.setGenre(truncate(info.getCategories().get(0), 190));
            } else {
                book.setGenre("General");
            }
            
            // Truncate cover URL if needed
            if (book.getCoverUrl() != null) {
                book.setCoverUrl(truncate(book.getCoverUrl(), 990));
            }
            
            // Rating info
            if (info.getAverageRating() != null) {
                book.setAverageRating(info.getAverageRating());
            }
            if (info.getRatingsCount() != null) {
                book.setRatingsCount(info.getRatingsCount());
            }
            
            return Optional.of(book);
        } catch (Exception e) {
            log.error("Error converting Google Book: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Book> convertOpenLibraryToEntity(OpenLibraryDoc doc) {
        try {
            if (doc.getTitle() == null) {
                return Optional.empty();
            }
            
            Book book = new Book();
            book.setTitle(truncate(doc.getTitle(), 490));
            book.setAuthor(truncate(doc.getAuthorName() != null ? String.join(", ", doc.getAuthorName()) : "Unknown", 490));
            
            // ISBN
            if (doc.getIsbn() != null && !doc.getIsbn().isEmpty()) {
                book.setIsbn(doc.getIsbn().get(0));
            }
            
            // Cover
            if (doc.getCoverId() != null) {
                book.setCoverUrl(getOpenLibraryCoverUrlById(doc.getCoverId(), "L"));
            } else if (book.getIsbn() != null) {
                book.setCoverUrl(getOpenLibraryCoverUrl(book.getIsbn(), "L"));
            }
            
            // First publish year
            if (doc.getFirstPublishYear() != null) {
                book.setPublishedDate(LocalDate.of(doc.getFirstPublishYear(), 1, 1));
            }
            
            // Genre from subjects
            if (doc.getSubject() != null && !doc.getSubject().isEmpty()) {
                book.setGenre(truncate(doc.getSubject().get(0), 190));
            } else {
                book.setGenre("General");
            }
            
            // Truncate cover URL if needed
            if (book.getCoverUrl() != null) {
                book.setCoverUrl(truncate(book.getCoverUrl(), 990));
            }
            
            book.setPageCount(doc.getNumberOfPagesMedian());
            book.setLanguage("en");
            
            return Optional.of(book);
        } catch (Exception e) {
            log.error("Error converting Open Library book: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private LocalDate parsePublishedDate(String dateStr) {
        try {
            if (dateStr.length() == 4) {
                return LocalDate.of(Integer.parseInt(dateStr), 1, 1);
            } else if (dateStr.length() == 7) {
                return LocalDate.parse(dateStr + "-01");
            } else {
                return LocalDate.parse(dateStr);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }

    /**
     * Combined search across both Google Books and Open Library APIs
     * Returns results without saving to database
     */
    public List<Book> searchExternalApis(String query, int limit) {
        log.info("Searching external APIs for: {}", query);
        
        // Search both APIs in parallel-ish manner
        List<Book> googleResults = searchGoogleBooks(query, limit);
        List<Book> openLibraryResults = searchOpenLibrary(query, limit);
        
        // Combine results, preferring Google (better covers) but supplementing with Open Library
        java.util.Map<String, Book> combined = new java.util.LinkedHashMap<>();
        
        // Add Google results first
        for (Book book : googleResults) {
            String key = normalizeKey(book.getTitle(), book.getAuthor());
            combined.put(key, book);
        }
        
        // Add Open Library results that aren't duplicates
        for (Book book : openLibraryResults) {
            String key = normalizeKey(book.getTitle(), book.getAuthor());
            if (!combined.containsKey(key)) {
                combined.put(key, book);
            }
        }
        
        return combined.values().stream().limit(limit).toList();
    }
    
    private String normalizeKey(String title, String author) {
        String t = title != null ? title.toLowerCase().replaceAll("[^a-z0-9]", "") : "";
        String a = author != null ? author.toLowerCase().replaceAll("[^a-z0-9]", "") : "";
        return t + "|" + a;
    }

    // ============ DTOs for Google Books API ============
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleBooksResponse {
        private List<GoogleBooksItem> items;
        private Integer totalItems;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleBooksItem {
        private String id;
        private GoogleVolumeInfo volumeInfo;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GoogleVolumeInfo {
        private String title;
        private List<String> authors;
        private String publisher;
        private String publishedDate;
        private String description;
        private List<IndustryIdentifier> industryIdentifiers;
        private Integer pageCount;
        private List<String> categories;
        private Double averageRating;
        private Integer ratingsCount;
        private ImageLinks imageLinks;
        private String language;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndustryIdentifier {
        private String type;
        private String identifier;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageLinks {
        private String smallThumbnail;
        private String thumbnail;
        private String small;
        private String medium;
        private String large;
    }

    // ============ DTOs for Open Library API ============
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenLibrarySearchResponse {
        private List<OpenLibraryDoc> docs;
        private Integer numFound;
    }
    
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenLibraryDoc {
        private String title;
        @JsonProperty("author_name")
        private List<String> authorName;
        @JsonProperty("first_publish_year")
        private Integer firstPublishYear;
        private List<String> isbn;
        @JsonProperty("cover_i")
        private Long coverId;
        private List<String> subject;
        @JsonProperty("number_of_pages_median")
        private Integer numberOfPagesMedian;
    }
}
