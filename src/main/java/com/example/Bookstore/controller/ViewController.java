package com.example.Bookstore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;

@Controller
public class ViewController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @GetMapping("/")
    public String index(HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        if (userType != null) {
            if ("CUSTOMER".equals(userType)) {
                return "redirect:/customer/dashboard";
            } else if ("EMPLOYEE".equals(userType)) {
                String role = (String) session.getAttribute("userRole");
                if ("ADMIN".equals(role)) {
                    return "redirect:/admin/dashboard";
                } else if ("CASHIER".equals(role)) {
                    return "redirect:/admin/orders";
                } else if ("WAREHOUSE".equals(role)) {
                    return "redirect:/admin/inventory";
                } else if ("SHIPPER".equals(role)) {
                    return "redirect:/admin/shipments";
                }
                return "redirect:/admin/dashboard";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard(HttpSession session) {
        if (!"CUSTOMER".equals(session.getAttribute("userType"))) {
            return "redirect:/login";
        }
        return "customer/dashboard";
    }

    @GetMapping("/admin/access-denied")
    public String accessDenied() {
        return "admin/access-denied";
    }
}

