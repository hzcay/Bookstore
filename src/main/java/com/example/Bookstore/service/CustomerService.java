package com.example.Bookstore.service;

import com.example.Bookstore.dto.CustomerDTO;
import com.example.Bookstore.dto.UpdateProfileRequest;
import com.example.Bookstore.entity.Customer;
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
    
    Optional<CustomerDTO> getCustomerByEmailForAuth(String email);
    
    void updateCustomerPoints(String customerId, Integer points);
    
    List<CustomerDTO> getActiveCustomers();

    void activateCustomerByEmail(String email);

    Customer getCurrentCustomer();

    Customer getCurrentCustomerOrNull();

    void updateProfile(String customerId, UpdateProfileRequest req);
}
