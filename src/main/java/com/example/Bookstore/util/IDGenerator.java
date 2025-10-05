package com.example.Bookstore.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class IDGenerator {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    
    public static String generateBookId() {
        return "BK" + LocalDateTime.now().format(DATE_FORMATTER) + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    public static String generateCustomerId() {
        return "CUS" + LocalDateTime.now().format(DATE_FORMATTER) + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    public static String generateOrderId() {
        return "ORD" + LocalDateTime.now().format(DATE_FORMATTER) + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    public static String generateOrderItemId() {
        return "OI" + LocalDateTime.now().format(DATE_FORMATTER) + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    public static String generateInventoryId() {
        return "INV" + LocalDateTime.now().format(DATE_FORMATTER) + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    public static String generateShipmentId() {
        return "SHIP" + LocalDateTime.now().format(DATE_FORMATTER) + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    public static String generatePromotionId() {
        return "PROMO" + LocalDateTime.now().format(DATE_FORMATTER) + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    public static String generateCategoryId() {
        return "CAT" + LocalDateTime.now().format(DATE_FORMATTER) + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    public static String generateAuthorId() {
        return "AUTH" + LocalDateTime.now().format(DATE_FORMATTER) + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    public static String generatePublisherId() {
        return "PUB" + LocalDateTime.now().format(DATE_FORMATTER) + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    public static String generateSupplierId() {
        return "SUP" + LocalDateTime.now().format(DATE_FORMATTER) + String.format("%03d", (int)(Math.random() * 1000));
    }
    
    public static String generateEmployeeId() {
        return "EMP" + LocalDateTime.now().format(DATE_FORMATTER) + String.format("%03d", (int)(Math.random() * 1000));
    }
}
