package com.shelve.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    private String avatarUrl;
    
    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio;
}
