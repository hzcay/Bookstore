package com.example.Bookstore.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// @Component  // Disabled to avoid conflict
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        // Cho phép truy cập các trang public
        if (isPublicPage(requestURI)) {
            return true;
        }
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userType") == null) {
            response.sendRedirect("/login");
            return false;
        }
        
        return true;
    }
    
    private boolean isPublicPage(String requestURI) {
        return requestURI.equals("/") ||
               requestURI.equals("/login") ||
               requestURI.equals("/register") ||
               requestURI.equals("/forgot-password") ||
               requestURI.startsWith("/api/v1/auth/") ||
               requestURI.startsWith("/api/v1/cart/") ||
               requestURI.startsWith("/api/v1/categories/") ||
               requestURI.startsWith("/static/") ||
               requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/");
    }
}