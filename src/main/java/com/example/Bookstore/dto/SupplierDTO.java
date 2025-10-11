package com.example.Bookstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDTO {
    private String supplierId;
    private String name;
    private String phone;
    private String address;
    private Double debt;
    private Integer status;
}

