package com.example.Bookstore.dto.warehouse;

import jakarta.validation.constraints.NotBlank;

public record SupplierRequest(
  @NotBlank String name,
  String address,
  String phone
) {}
