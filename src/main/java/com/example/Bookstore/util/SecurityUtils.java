package com.example.Bookstore.util;

import jakarta.servlet.http.HttpSession;

public class SecurityUtils {
    
    public static boolean isAdmin(HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        return "ADMIN".equals(userRole);
    }
    
    public static boolean isCashier(HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        return "CASHIER".equals(userRole);
    }
    
    public static boolean isWarehouse(HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        return "WAREHOUSE".equals(userRole);
    }
    
    public static boolean isShipper(HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        return "SHIPPER".equals(userRole);
    }
    
    public static boolean hasRole(HttpSession session, String role) {
        String userRole = (String) session.getAttribute("userRole");
        return role.equals(userRole);
    }
    
    public static boolean hasAnyRole(HttpSession session, String... roles) {
        String userRole = (String) session.getAttribute("userRole");
        if (userRole == null) return false;
        
        for (String role : roles) {
            if (role.equals(userRole)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean isEmployee(HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        return "EMPLOYEE".equals(userType);
    }
    
    public static boolean isCustomer(HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        return "CUSTOMER".equals(userType);
    }
}
