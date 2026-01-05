package com.shelve.shelf.dto;

import com.shelve.shelf.entity.ShelfType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveBookRequest {
    @NotNull(message = "Target shelf is required")
    private ShelfType targetShelf;
}
