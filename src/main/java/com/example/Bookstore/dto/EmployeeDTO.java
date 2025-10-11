package com.example.Bookstore.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDTO {
    private String employeeId;
    private String name;
    private String role;
    private String phone;
    private String email;
    private String password;
    private Integer status;
}

