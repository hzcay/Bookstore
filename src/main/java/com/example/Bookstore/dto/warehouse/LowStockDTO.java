package com.example.Bookstore.dto.warehouse;

public record LowStockDTO(
  String bookId,
  String title,
  Long stock
) {}

