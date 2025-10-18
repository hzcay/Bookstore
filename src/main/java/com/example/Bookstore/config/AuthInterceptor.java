package com.example.Bookstore.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        HttpSession session = request.getSession(false);

        if (uri.startsWith("/admin")) {
            if (session == null || !"EMPLOYEE".equals(session.getAttribute("userType"))) {
                response.sendRedirect("/login");
                return false;
            }

            String role = (String) session.getAttribute("userRole");
            
            if (uri.contains("/dashboard") || uri.contains("/reports")) {
                if (!"ADMIN".equals(role)) {
                    response.sendRedirect("/admin/access-denied");
                    return false;
                }
            } else if (uri.contains("/employees") || uri.contains("/suppliers") || 
                       uri.contains("/categories") || uri.contains("/authors") || uri.contains("/publishers")) {
                if (!"ADMIN".equals(role)) {
                    response.sendRedirect("/admin/access-denied");
                    return false;
                }
            } else if (uri.contains("/inventory") || uri.contains("/shipments")) {
                if (!"ADMIN".equals(role) && !"WAREHOUSE".equals(role)) {
                    response.sendRedirect("/admin/access-denied");
                    return false;
                }
            } else if (uri.contains("/orders")) {
                if (!"ADMIN".equals(role) && !"CASHIER".equals(role)) {
                    response.sendRedirect("/admin/access-denied");
                    return false;
                }
            }
        } else if (uri.startsWith("/customer")) {
            if (session == null || !"CUSTOMER".equals(session.getAttribute("userType"))) {
                response.sendRedirect("/login");
                return false;
            }
        }

        return true;
    }
}

