package com.shelve.review.event;

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
public class RatingEvent implements Serializable {
    private String userId;
    private String bookId;
    private Integer score;
    private boolean liked;
    private String eventType; // CREATED, UPDATED
    private LocalDateTime timestamp;
}
