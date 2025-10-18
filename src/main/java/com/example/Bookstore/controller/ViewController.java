package com.example.Bookstore.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import static com.example.Bookstore.controller.AuthController.SESSION_UID;

@Controller
@RequiredArgsConstructor
public class ViewController {

    @Value("${app.api-base:/api/v1}")
    private String apiBase;

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
    public String root(HttpSession session) {
        return (session.getAttribute(SESSION_UID) == null)
                ? "redirect:/login"
                : "redirect:/homePage";
    }

    @GetMapping({ "/homePage" })
    public String homePage(Model model, HttpServletRequest req) {
        model.addAttribute("uri", "/homePage");
        model.addAttribute("apiBase", apiBase);
        return "index";
    }

    @GetMapping({ "/browse", "/books" })
    public String browse(Model model, HttpServletRequest req) {
        model.addAttribute("uri", req.getRequestURI());
        model.addAttribute("apiBase", apiBase);
        return "index";
    }

    @GetMapping("/book/{id}")
    public String bookDetail(@PathVariable String id, Model model, HttpServletRequest req) {
        model.addAttribute("uri", "/homePage");
        model.addAttribute("apiBase", apiBase);
        model.addAttribute("bookId", id);
        return "book";
    }

    @GetMapping("/cart")
    public String cart(Model model, HttpServletRequest req) {
        model.addAttribute("uri", req.getRequestURI());
        model.addAttribute("apiBase", apiBase);
        return "cart";
    }

    @GetMapping("/checkout")
    public String checkout(Model model, HttpServletRequest req) {
        model.addAttribute("uri", req.getRequestURI());
        model.addAttribute("apiBase", apiBase);
        return "checkout";
    }

    @GetMapping("/track")
    public String track(Model model, HttpServletRequest req) {
        model.addAttribute("uri", req.getRequestURI());
        model.addAttribute("apiBase", apiBase);
        return "track";
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard(HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        if (!"CUSTOMER".equals(userType)) {
            return "redirect:/login";
        }
        return "dashboard";
    }

    @GetMapping("/shipper/dashboard")
    public String shipperDashboard(HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        String userRole = (String) session.getAttribute("userRole");
        if (!"EMPLOYEE".equals(userType) || !"SHIPPER".equals(userRole)) {
            return "redirect:/login";
        }
        return "redirect:/shipper/home";
    }

    @GetMapping("/admin/access-denied")
    public String accessDenied() {
        return "admin/access-denied";
    }
}

