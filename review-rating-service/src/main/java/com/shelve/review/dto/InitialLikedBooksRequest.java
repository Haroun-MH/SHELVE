package com.shelve.review.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class InitialLikedBooksRequest {
    @NotEmpty(message = "At least one book must be selected")
    private List<String> bookIds;
}
