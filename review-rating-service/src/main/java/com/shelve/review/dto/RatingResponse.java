package com.shelve.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {
    private String id;
    private String userId;
    private String bookId;
    private Integer score;
    private boolean liked;
    private LocalDateTime createdAt;
}
