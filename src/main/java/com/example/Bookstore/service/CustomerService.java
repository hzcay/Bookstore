package com.example.Bookstore.service;

import com.example.Bookstore.dto.CustomerDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CustomerService {
    
    Page<CustomerDTO> getAllCustomers(String searchTerm, Pageable pageable);
    
    Optional<CustomerDTO> getCustomerById(String customerId);
    
    CustomerDTO createCustomer(CustomerDTO customerDTO);
    
    CustomerDTO updateCustomer(String customerId, CustomerDTO customerDTO);
    
    void deleteCustomer(String customerId);
    
    Optional<CustomerDTO> getCustomerByPhone(String phone);
    
    Optional<CustomerDTO> getCustomerByEmail(String email);
    
    void updateCustomerPoints(String customerId, Integer points);
    
    List<CustomerDTO> getActiveCustomers();
}
