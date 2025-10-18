package com.example.Bookstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**","/swagger-config.json").permitAll()
                .requestMatchers("/", "/error").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/login", "/register", "/forgot-password", "/verify-otp").permitAll()
                .requestMatchers("/admin/**").permitAll()
                .requestMatchers("/customer/**").permitAll()
                .requestMatchers("/api/v1/books/**").permitAll()
                .requestMatchers("/api/v1/categories/**").permitAll()
                .requestMatchers("/api/v1/authors/**").permitAll()
                .requestMatchers("/api/v1/publishers/**").permitAll()
                .requestMatchers("/api/v1/promotions/**").permitAll()
                .requestMatchers("/api/v1/customers/**").permitAll()
                .requestMatchers("/api/v1/orders/**").permitAll()
                .requestMatchers("/api/v1/shipments/**").permitAll()
                .requestMatchers("/api/v1/inventory/**").permitAll()
                .requestMatchers("/api/v1/suppliers/**").permitAll()
                .requestMatchers("/api/v1/employees/**").permitAll()
                .requestMatchers("/api/v1/reports/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
            );
        
        return http.build();
    }
}
