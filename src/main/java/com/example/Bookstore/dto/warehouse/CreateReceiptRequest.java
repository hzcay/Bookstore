package com.example.Bookstore.dto.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateReceiptRequest(
  @NotBlank String bookId,
  @NotBlank String supplierId,
  @Min(1) int quantity
) {}

