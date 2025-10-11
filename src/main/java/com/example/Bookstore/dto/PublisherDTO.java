package com.example.Bookstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublisherDTO {
    
    private String publisherId;
    private String name;
    private String address;
    private String phone;
    private Integer status;
}

