package com.example.Bookstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDTO {
    
    private String authorId;
    private String name;
    private String description;
    private Integer status;
}

