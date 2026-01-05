package com.shelve.shelf.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShelfEvent implements Serializable {
    private String userId;
    private String bookId;
    private String shelfType;
    private String eventType; // ADDED, MOVED, REMOVED
    private String previousShelfType;
    private LocalDateTime timestamp;
}
