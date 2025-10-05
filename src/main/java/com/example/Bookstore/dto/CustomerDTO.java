package com.example.Bookstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {
    
    private String customerId;
    private String name;
    private String phone;
    private String email;
    private String address;
    private Integer points;
    private Integer status;
}
