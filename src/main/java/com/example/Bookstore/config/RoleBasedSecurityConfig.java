package com.example.Bookstore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.Bookstore.repository.EmployeeRepository;
import org.springframework.security.core.userdetails.User;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Configuration
public class RoleBasedSecurityConfig {

    // @Bean  // Disabled to avoid conflict with SecurityConfig
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // @Bean  // Disabled to avoid conflict with SecurityConfig
    public UserDetailsService userDetailsService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        return new CustomUserDetailsService(employeeRepository, passwordEncoder);
    }

    public static class CustomUserDetailsService implements UserDetailsService {
        private final EmployeeRepository employeeRepository;
        private final PasswordEncoder passwordEncoder;
        
        public CustomUserDetailsService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
            this.employeeRepository = employeeRepository;
            this.passwordEncoder = passwordEncoder;
        }
        
        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            return employeeRepository.findByEmail(username)
                    .map(emp -> User.withUsername(emp.getEmail())
                            .password(emp.getPassword())
                            .roles(emp.getRole())
                            .build())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        }
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    public static boolean hasAnyRole(String... roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
                .anyMatch(authority -> {
                    String authorityRole = authority.getAuthority();
                    return Arrays.stream(roles)
                            .anyMatch(role -> authorityRole.equals("ROLE_" + role));
                });
    }
}
