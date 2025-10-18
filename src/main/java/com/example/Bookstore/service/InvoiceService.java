package com.example.Bookstore.service;

import com.example.Bookstore.entity.Order;

public interface InvoiceService {
    byte[] generateInvoicePDF(Order order) throws Exception;
}
