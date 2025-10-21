package com.example.Bookstore.dto.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateReceiptRequest(
  @NotBlank String bookId,
  @NotBlank String supplierId,
  @Min(1) int quantity,
  @NotNull @Min(0) Double importPrice  // Giá nhập/đơn vị
) {}

