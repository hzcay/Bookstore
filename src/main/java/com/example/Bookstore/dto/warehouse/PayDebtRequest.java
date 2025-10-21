package com.example.Bookstore.dto.warehouse;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PayDebtRequest(
  @NotBlank String supplierId,
  @NotNull @Min(1) Double paymentAmount,
  String paymentMethod,
  String note,
  String employeeName
) {}
