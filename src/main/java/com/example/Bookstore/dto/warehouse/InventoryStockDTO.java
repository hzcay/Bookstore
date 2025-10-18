package com.example.Bookstore.dto.warehouse;

public record InventoryStockDTO(
  String bookId,
  String title,
  Long stock
) {}

