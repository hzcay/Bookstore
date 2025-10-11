package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    
    @Query("SELECT c FROM Customer c WHERE " +
           "(:q IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.phone) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Customer> searchCustomers(@Param("q") String q, Pageable pageable);
    
    List<Customer> findByStatus(Integer status);
    
    Optional<Customer> findByPhone(String phone);
    
    Optional<Customer> findByEmail(String email);
    
    Optional<Customer> findByCustomerIdAndStatus(String customerId, Integer status);
    
    boolean existsByPhoneAndStatus(String phone, Integer status);
    
    boolean existsByEmailAndStatus(String email, Integer status);
}
