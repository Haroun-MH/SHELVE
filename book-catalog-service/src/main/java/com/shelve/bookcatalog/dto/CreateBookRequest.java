package com.shelve.bookcatalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateBookRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Author is required")
    private String author;
    
    private String isbn;
    
    private String description;
    
    private String coverUrl;
    
    @NotBlank(message = "Genre is required")
    private String genre;
    
    private LocalDate publishedDate;
    
    private Integer pageCount;
    
    private String publisher;
    
    private String language;
}
