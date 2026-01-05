package com.shelve.shelf.service;

import com.shelve.shelf.client.BookClient;
import com.shelve.shelf.dto.BookResponse;
import com.shelve.shelf.dto.ShelfItemResponse;
import com.shelve.shelf.dto.ShelvesResponse;
import com.shelve.shelf.entity.ShelfItem;
import com.shelve.shelf.entity.ShelfType;
import com.shelve.shelf.event.ShelfEvent;
import com.shelve.shelf.exception.BookAlreadyOnShelfException;
import com.shelve.shelf.exception.ShelfItemNotFoundException;
import com.shelve.shelf.repository.ShelfItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShelfService {

    private final ShelfItemRepository shelfItemRepository;
    private final BookClient bookClient;
    private final ShelfEventPublisher shelfEventPublisher;

    public ShelvesResponse getUserShelves(String userId) {
        UUID userUUID = UUID.fromString(userId);
        
        List<ShelfItem> allItems = shelfItemRepository.findByUserId(userUUID);
        
        // Get all book IDs
        List<String> bookIds = allItems.stream()
                .map(item -> item.getBookId().toString())
                .collect(Collectors.toList());
        
        // Fetch book details
        Map<String, BookResponse> booksMap = new HashMap<>();
        if (!bookIds.isEmpty()) {
            List<BookResponse> books = bookClient.getBooksByIds(bookIds);
            booksMap = books.stream()
                    .collect(Collectors.toMap(BookResponse::getId, b -> b));
        }
        
        // Group by shelf type
        Map<ShelfType, List<ShelfItem>> groupedItems = allItems.stream()
                .collect(Collectors.groupingBy(ShelfItem::getShelfType));
        
        Map<String, BookResponse> finalBooksMap = booksMap;
        
        return ShelvesResponse.builder()
                .reading(mapToResponses(groupedItems.getOrDefault(ShelfType.READING, List.of()), finalBooksMap))
                .read(mapToResponses(groupedItems.getOrDefault(ShelfType.READ, List.of()), finalBooksMap))
                .toRead(mapToResponses(groupedItems.getOrDefault(ShelfType.TO_READ, List.of()), finalBooksMap))
                .stats(ShelvesResponse.ShelfStats.builder()
                        .readingCount(shelfItemRepository.countByUserIdAndShelfType(userUUID, ShelfType.READING))
                        .readCount(shelfItemRepository.countByUserIdAndShelfType(userUUID, ShelfType.READ))
                        .toReadCount(shelfItemRepository.countByUserIdAndShelfType(userUUID, ShelfType.TO_READ))
                        .build())
                .build();
    }

    public List<ShelfItemResponse> getShelfByType(String userId, ShelfType shelfType) {
        UUID userUUID = UUID.fromString(userId);
        List<ShelfItem> items = shelfItemRepository.findByUserIdAndShelfType(userUUID, shelfType);
        
        List<String> bookIds = items.stream()
                .map(item -> item.getBookId().toString())
                .collect(Collectors.toList());
        
        Map<String, BookResponse> booksMap = new HashMap<>();
        if (!bookIds.isEmpty()) {
            List<BookResponse> books = bookClient.getBooksByIds(bookIds);
            booksMap = books.stream()
                    .collect(Collectors.toMap(BookResponse::getId, b -> b));
        }
        
        return mapToResponses(items, booksMap);
    }

    @Transactional
    public ShelfItemResponse addBookToShelf(String userId, String bookId, ShelfType shelfType) {
        UUID userUUID = UUID.fromString(userId);
        UUID bookUUID = UUID.fromString(bookId);
        
        // Check if book already on a shelf
        Optional<ShelfItem> existing = shelfItemRepository.findByUserIdAndBookId(userUUID, bookUUID);
        if (existing.isPresent()) {
            throw new BookAlreadyOnShelfException("Book is already on shelf: " + existing.get().getShelfType());
        }
        
        ShelfItem item = ShelfItem.builder()
                .userId(userUUID)
                .bookId(bookUUID)
                .shelfType(shelfType)
                .build();
        
        if (shelfType == ShelfType.READING) {
            item.setStartedAt(LocalDateTime.now());
        } else if (shelfType == ShelfType.READ) {
            item.setFinishedAt(LocalDateTime.now());
        }
        
        item = shelfItemRepository.save(item);
        
        // Publish shelf event for recommendation updates (especially for READ shelf)
        publishShelfEvent(userId, bookId, shelfType.name(), "ADDED", null);
        
        // Fetch book details
        List<BookResponse> books = bookClient.getBooksByIds(List.of(bookId));
        BookResponse book = books.isEmpty() ? null : books.get(0);
        
        return mapToResponse(item, book);
    }

    @Transactional
    public ShelfItemResponse moveBook(String userId, String bookId, ShelfType targetShelf) {
        UUID userUUID = UUID.fromString(userId);
        UUID bookUUID = UUID.fromString(bookId);
        
        ShelfItem item = shelfItemRepository.findByUserIdAndBookId(userUUID, bookUUID)
                .orElseThrow(() -> new ShelfItemNotFoundException("Book not found on any shelf"));
        
        ShelfType previousShelf = item.getShelfType();
        item.setShelfType(targetShelf);
        
        // Update timestamps based on shelf transition
        if (targetShelf == ShelfType.READING && previousShelf != ShelfType.READING) {
            item.setStartedAt(LocalDateTime.now());
        } else if (targetShelf == ShelfType.READ && previousShelf != ShelfType.READ) {
            item.setFinishedAt(LocalDateTime.now());
        }
        
        item = shelfItemRepository.save(item);
        
        // Publish shelf event for recommendation updates
        publishShelfEvent(userId, bookId, targetShelf.name(), "MOVED", previousShelf.name());
        
        // Fetch book details
        List<BookResponse> books = bookClient.getBooksByIds(List.of(bookId));
        BookResponse book = books.isEmpty() ? null : books.get(0);
        
        return mapToResponse(item, book);
    }

    @Transactional
    public void removeBookFromShelf(String userId, String bookId) {
        UUID userUUID = UUID.fromString(userId);
        UUID bookUUID = UUID.fromString(bookId);
        
        if (!shelfItemRepository.existsByUserIdAndBookId(userUUID, bookUUID)) {
            throw new ShelfItemNotFoundException("Book not found on any shelf");
        }
        
        shelfItemRepository.deleteByUserIdAndBookId(userUUID, bookUUID);
    }

    public Optional<ShelfItemResponse> getBookShelfStatus(String userId, String bookId) {
        UUID userUUID = UUID.fromString(userId);
        UUID bookUUID = UUID.fromString(bookId);
        
        return shelfItemRepository.findByUserIdAndBookId(userUUID, bookUUID)
                .map(item -> {
                    List<BookResponse> books = bookClient.getBooksByIds(List.of(bookId));
                    BookResponse book = books.isEmpty() ? null : books.get(0);
                    return mapToResponse(item, book);
                });
    }

    private List<ShelfItemResponse> mapToResponses(List<ShelfItem> items, Map<String, BookResponse> booksMap) {
        return items.stream()
                .map(item -> mapToResponse(item, booksMap.get(item.getBookId().toString())))
                .collect(Collectors.toList());
    }

    private ShelfItemResponse mapToResponse(ShelfItem item, BookResponse book) {
        return ShelfItemResponse.builder()
                .id(item.getId().toString())
                .bookId(item.getBookId().toString())
                .book(book)
                .shelfType(item.getShelfType())
                .startedAt(item.getStartedAt())
                .finishedAt(item.getFinishedAt())
                .addedAt(item.getAddedAt())
                .build();
    }
    
    private void publishShelfEvent(String userId, String bookId, String shelfType, String eventType, String previousShelf) {
        try {
            ShelfEvent event = ShelfEvent.builder()
                    .userId(userId)
                    .bookId(bookId)
                    .shelfType(shelfType)
                    .eventType(eventType)
                    .previousShelfType(previousShelf)
                    .timestamp(LocalDateTime.now())
                    .build();
            shelfEventPublisher.publishShelfEvent(event);
        } catch (Exception e) {
            log.warn("Failed to publish shelf event: {}", e.getMessage());
        }
    }
}
