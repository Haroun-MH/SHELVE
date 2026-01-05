package com.shelve.shelf.dto;

import com.shelve.shelf.entity.ShelfType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShelfItemResponse {
    private String id;
    private String bookId;
    private BookResponse book;
    private ShelfType shelfType;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime addedAt;
}
