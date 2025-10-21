package com.example.Bookstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank
    private String name;

    @Email @NotBlank
    private String email;

    private String phone;
    private String address;

    // để trống nếu không đổi
    private String password;

    // --- match các th:field trong profile.html ---
    private String favoriteGenres;       // th:field="*{favoriteGenres}"
    private String favoritePublishers;   // th:field="*{favoritePublishers}"
    private Boolean emailNotify;         // th:field="*{emailNotify}"
    private Boolean smsNotify;           // th:field="*{smsNotify}"

    // Avatar "tượng trưng"
    private String avatarUrl;            // dùng cho th:src ở avatarPreview
}

