package com.shelve.bookcatalog.config;

import com.shelve.bookcatalog.entity.Book;
import com.shelve.bookcatalog.repository.BookRepository;
import com.shelve.bookcatalog.service.ExternalBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class DataInitializer {

    @Value("${book-import.on-startup:true}")
    private boolean importOnStartup;

    @Value("${book-import.use-api:true}")
    private boolean useApi;

    // Diverse search queries to populate catalog with popular books from both Google Books and Open Library
    private static final String[] GOOGLE_BOOKS_QUERIES = {
        "bestseller fiction 2024",
        "bestseller fiction 2023",
        "classic literature must read",
        "science fiction award winning",
        "fantasy epic series",
        "mystery thriller bestseller",
        "romance popular novels",
        "historical fiction acclaimed",
        "biography inspiring",
        "self improvement bestseller",
        "popular science books",
        "young adult fiction",
        "horror stephen king",
        "poetry classics",
        "philosophy introduction",
        "psychology popular",
        "business leadership",
        "travel adventure",
        "cooking chef"
    };
    
    private static final String[] OPEN_LIBRARY_QUERIES = {
        "harry potter",
        "lord of the rings",
        "game of thrones",
        "percy jackson",
        "hunger games",
        "divergent",
        "twilight",
        "maze runner",
        "narnia",
        "sherlock holmes"
    };

    @Bean
    CommandLineRunner initDatabase(BookRepository bookRepository, ExternalBookService externalBookService) {
        return args -> {
            if (bookRepository.count() == 0) {
                log.info("Database is empty. Initializing book catalog...");

                if (importOnStartup && useApi) {
                    importFromGoogleBooks(bookRepository, externalBookService);
                } else {
                    loadFallbackData(bookRepository);
                }
            } else {
                log.info("Book catalog already contains {} books", bookRepository.count());
            }
        };
    }

    /**
     * Import books from Google Books and Open Library APIs on startup
     */
    private void importFromGoogleBooks(BookRepository bookRepository, ExternalBookService externalBookService) {
        log.info("Fetching books from external APIs...");
        int totalImported = 0;

        // Import from Google Books
        for (String query : GOOGLE_BOOKS_QUERIES) {
            try {
                log.info("Searching Google Books for: '{}'", query);
                List<Book> books = externalBookService.searchGoogleBooks(query, 15);

                for (Book book : books) {
                    if (saveBookIfNew(bookRepository, book)) {
                        totalImported++;
                        log.debug("Imported from Google: {} by {}", book.getTitle(), book.getAuthor());
                    }
                }

                // Rate limiting - avoid hitting API too fast
                Thread.sleep(200);

            } catch (Exception e) {
                log.warn("Failed to fetch books for query '{}': {}", query, e.getMessage());
            }
        }
        
        log.info("Imported {} books from Google Books API", totalImported);
        
        // Import from Open Library for popular series
        int openLibraryImported = 0;
        for (String query : OPEN_LIBRARY_QUERIES) {
            try {
                log.info("Searching Open Library for: '{}'", query);
                List<Book> books = externalBookService.searchOpenLibrary(query, 10);

                for (Book book : books) {
                    if (saveBookIfNew(bookRepository, book)) {
                        openLibraryImported++;
                        log.debug("Imported from Open Library: {} by {}", book.getTitle(), book.getAuthor());
                    }
                }

                Thread.sleep(200);

            } catch (Exception e) {
                log.warn("Failed to fetch books from Open Library for '{}': {}", query, e.getMessage());
            }
        }
        
        log.info("Imported {} books from Open Library API", openLibraryImported);
        totalImported += openLibraryImported;

        log.info("Successfully imported {} total books from external APIs", totalImported);

        // If we got fewer than 30 books, supplement with fallback data
        if (totalImported < 30) {
            log.info("APIs returned fewer books than expected, loading fallback data...");
            loadFallbackData(bookRepository);
        }
    }
    
    private boolean saveBookIfNew(BookRepository bookRepository, Book book) {
        try {
            // Skip duplicates
            if (book.getIsbn() != null && bookRepository.existsByIsbn(book.getIsbn())) {
                return false;
            }
            if (bookRepository.existsByTitleAndAuthor(book.getTitle(), book.getAuthor())) {
                return false;
            }
            bookRepository.save(book);
            return true;
        } catch (Exception e) {
            log.warn("Failed to save book '{}': {}", book.getTitle(), e.getMessage());
            return false;
        }
    }

    /**
     * Fallback: Load curated book list if API is unavailable or disabled
     */
    private void loadFallbackData(BookRepository bookRepository) {
        log.info("Loading fallback book catalog...");
        List<Book> fallbackBooks = createFallbackBooks();
        int saved = 0;

        for (Book book : fallbackBooks) {
            if (book.getIsbn() != null && bookRepository.existsByIsbn(book.getIsbn())) {
                continue;
            }
            if (bookRepository.existsByTitleAndAuthor(book.getTitle(), book.getAuthor())) {
                continue;
            }
            bookRepository.save(book);
            saved++;
        }

        log.info("Loaded {} fallback books", saved);
    }

    /**
     * Creates a curated list of popular books across genres
     */
    private List<Book> createFallbackBooks() {
        List<Book> books = new ArrayList<>();

        // Fiction
        books.add(Book.builder()
            .title("To Kill a Mockingbird").author("Harper Lee").isbn("9780061120084")
            .description("A classic of modern American literature about racial injustice in the Deep South.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780061120084-L.jpg")
            .genre("Fiction").publishedDate(LocalDate.of(1960, 7, 11)).pageCount(281)
            .publisher("J. B. Lippincott & Co.").language("en").averageRating(4.5).ratingsCount(1250).build());

        books.add(Book.builder()
            .title("The Great Gatsby").author("F. Scott Fitzgerald").isbn("9780743273565")
            .description("A tragic love story set in the Jazz Age exploring the American Dream.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780743273565-L.jpg")
            .genre("Fiction").publishedDate(LocalDate.of(1925, 4, 10)).pageCount(180)
            .publisher("Charles Scribner's Sons").language("en").averageRating(4.3).ratingsCount(890).build());

        // Science Fiction
        books.add(Book.builder()
            .title("1984").author("George Orwell").isbn("9780452284234")
            .description("A dystopian masterpiece about totalitarianism and the manipulation of truth.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780452284234-L.jpg")
            .genre("Science Fiction").publishedDate(LocalDate.of(1949, 6, 8)).pageCount(328)
            .publisher("Secker & Warburg").language("en").averageRating(4.4).ratingsCount(980).build());

        books.add(Book.builder()
            .title("Dune").author("Frank Herbert").isbn("9780441172719")
            .description("An epic science fiction saga set on the desert planet Arrakis.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780441172719-L.jpg")
            .genre("Science Fiction").publishedDate(LocalDate.of(1965, 8, 1)).pageCount(688)
            .publisher("Chilton Books").language("en").averageRating(4.7).ratingsCount(1500).build());

        books.add(Book.builder()
            .title("The Hunger Games").author("Suzanne Collins").isbn("9780439023481")
            .description("A dystopian novel about a televised fight to the death.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780439023481-L.jpg")
            .genre("Science Fiction").publishedDate(LocalDate.of(2008, 9, 14)).pageCount(374)
            .publisher("Scholastic Press").language("en").averageRating(4.5).ratingsCount(1800).build());

        // Fantasy
        books.add(Book.builder()
            .title("The Hobbit").author("J.R.R. Tolkien").isbn("9780618002214")
            .description("A fantasy adventure following Bilbo Baggins on an unexpected journey.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780618002214-L.jpg")
            .genre("Fantasy").publishedDate(LocalDate.of(1937, 9, 21)).pageCount(310)
            .publisher("George Allen & Unwin").language("en").averageRating(4.8).ratingsCount(2000).build());

        books.add(Book.builder()
            .title("Harry Potter and the Sorcerer's Stone").author("J.K. Rowling").isbn("9780590353403")
            .description("A young wizard discovers his magical heritage at Hogwarts School.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780590353403-L.jpg")
            .genre("Fantasy").publishedDate(LocalDate.of(1997, 6, 26)).pageCount(309)
            .publisher("Bloomsbury").language("en").averageRating(4.9).ratingsCount(3500).build());

        books.add(Book.builder()
            .title("A Game of Thrones").author("George R.R. Martin").isbn("9780553103540")
            .description("Epic fantasy of noble houses fighting for the Iron Throne.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780553103540-L.jpg")
            .genre("Fantasy").publishedDate(LocalDate.of(1996, 8, 1)).pageCount(694)
            .publisher("Bantam Spectra").language("en").averageRating(4.6).ratingsCount(1800).build());

        // Mystery/Thriller
        books.add(Book.builder()
            .title("Gone Girl").author("Gillian Flynn").isbn("9780307588364")
            .description("A psychological thriller about a husband suspected of his wife's disappearance.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780307588364-L.jpg")
            .genre("Thriller").publishedDate(LocalDate.of(2012, 6, 5)).pageCount(415)
            .publisher("Crown Publishing").language("en").averageRating(4.2).ratingsCount(1100).build());

        books.add(Book.builder()
            .title("The Girl with the Dragon Tattoo").author("Stieg Larsson").isbn("9780307269751")
            .description("A journalist and hacker investigate a decades-old disappearance.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780307269751-L.jpg")
            .genre("Mystery").publishedDate(LocalDate.of(2005, 8, 1)).pageCount(465)
            .publisher("Norstedts Förlag").language("en").averageRating(4.3).ratingsCount(950).build());

        // Romance
        books.add(Book.builder()
            .title("Pride and Prejudice").author("Jane Austen").isbn("9780141439518")
            .description("A romantic novel following Elizabeth Bennet and Mr. Darcy.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780141439518-L.jpg")
            .genre("Romance").publishedDate(LocalDate.of(1813, 1, 28)).pageCount(432)
            .publisher("T. Egerton").language("en").averageRating(4.6).ratingsCount(1100).build());

        books.add(Book.builder()
            .title("Outlander").author("Diana Gabaldon").isbn("9780440212560")
            .description("A WWII nurse travels back in time to 18th-century Scotland.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780440212560-L.jpg")
            .genre("Romance").publishedDate(LocalDate.of(1991, 6, 1)).pageCount(850)
            .publisher("Delacorte Press").language("en").averageRating(4.5).ratingsCount(1300).build());

        // Non-Fiction / Self-Help
        books.add(Book.builder()
            .title("Atomic Habits").author("James Clear").isbn("9780735211292")
            .description("A practical guide to building good habits and breaking bad ones.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780735211292-L.jpg")
            .genre("Self-Help").publishedDate(LocalDate.of(2018, 10, 16)).pageCount(320)
            .publisher("Avery").language("en").averageRating(4.8).ratingsCount(2200).build());

        books.add(Book.builder()
            .title("Sapiens: A Brief History of Humankind").author("Yuval Noah Harari").isbn("9780062316097")
            .description("An exploration of how Homo sapiens came to dominate the world.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780062316097-L.jpg")
            .genre("Non-Fiction").publishedDate(LocalDate.of(2011, 1, 1)).pageCount(443)
            .publisher("Harper").language("en").averageRating(4.6).ratingsCount(1700).build());

        books.add(Book.builder()
            .title("Thinking, Fast and Slow").author("Daniel Kahneman").isbn("9780374275631")
            .description("A groundbreaking look at the two systems that drive how we think.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780374275631-L.jpg")
            .genre("Psychology").publishedDate(LocalDate.of(2011, 10, 25)).pageCount(499)
            .publisher("Farrar, Straus and Giroux").language("en").averageRating(4.5).ratingsCount(1400).build());

        // Biography
        books.add(Book.builder()
            .title("Steve Jobs").author("Walter Isaacson").isbn("9781451648539")
            .description("The definitive biography of Apple co-founder Steve Jobs.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9781451648539-L.jpg")
            .genre("Biography").publishedDate(LocalDate.of(2011, 10, 24)).pageCount(656)
            .publisher("Simon & Schuster").language("en").averageRating(4.4).ratingsCount(1300).build());

        books.add(Book.builder()
            .title("Becoming").author("Michelle Obama").isbn("9781524763138")
            .description("The memoir of former First Lady Michelle Obama.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9781524763138-L.jpg")
            .genre("Biography").publishedDate(LocalDate.of(2018, 11, 13)).pageCount(448)
            .publisher("Crown Publishing").language("en").averageRating(4.7).ratingsCount(1600).build());

        // Modern bestsellers
        books.add(Book.builder()
            .title("Where the Crawdads Sing").author("Delia Owens").isbn("9780735219090")
            .description("A coming-of-age mystery set in the marshes of North Carolina.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780735219090-L.jpg")
            .genre("Fiction").publishedDate(LocalDate.of(2018, 8, 14)).pageCount(368)
            .publisher("G.P. Putnam's Sons").language("en").averageRating(4.6).ratingsCount(1550).build());

        books.add(Book.builder()
            .title("Project Hail Mary").author("Andy Weir").isbn("9780593135204")
            .description("An astronaut wakes up alone on a spacecraft with no memory.")
            .coverUrl("https://covers.openlibrary.org/b/isbn/9780593135204-L.jpg")
            .genre("Science Fiction").publishedDate(LocalDate.of(2021, 5, 4)).pageCount(496)
            .publisher("Ballantine Books").language("en").averageRating(4.9).ratingsCount(1900).build());

        return books;
    }
}