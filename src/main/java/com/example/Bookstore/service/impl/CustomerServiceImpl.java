package com.example.Bookstore.service.impl;

import com.example.Bookstore.dto.CustomerDTO;
import com.example.Bookstore.entity.Customer;
import com.example.Bookstore.repository.CustomerRepository;
import com.example.Bookstore.service.CustomerService;
import com.example.Bookstore.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    private CustomerRepository customerRepository;
    
    @Autowired
    private AuthService authService;

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerDTO> getAllCustomers(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return customerRepository.findAll(pageable).map(this::toDTO);
        }
        return customerRepository.searchCustomers(searchTerm, pageable).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerDTO> getCustomerById(String customerId) {
        return customerRepository.findByCustomerIdAndStatus(customerId, 1).map(this::toDTO);
    }

    @Override
    public CustomerDTO createCustomer(CustomerDTO customerDTO) {
        Optional<Customer> existingCustomer = customerRepository.findByEmail(customerDTO.getEmail());
        
        if (existingCustomer.isPresent()) {
            Customer c = existingCustomer.get();
            if (c.getStatus() == 0) {
                c.setStatus(0);
                c.setName(customerDTO.getName());
                c.setPhone(customerDTO.getPhone());
                c.setAddress(customerDTO.getAddress());
                c.setPoints(customerDTO.getPoints());
                c = customerRepository.save(c);
                
                return toDTO(c);
            } else {
                throw new RuntimeException("Customer already exists");
            }
        }
        
        Customer c = toEntity(customerDTO);
        c.setStatus(0);
        c = customerRepository.save(c);
        return toDTO(c);
    }

    @Override
    public CustomerDTO updateCustomer(String customerId, CustomerDTO customerDTO) {
        Customer c = customerRepository.findByCustomerIdAndStatus(customerId, 1)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (customerDTO.getName() != null) c.setName(customerDTO.getName());
        if (customerDTO.getPhone() != null) c.setPhone(customerDTO.getPhone());
        if (customerDTO.getEmail() != null) c.setEmail(customerDTO.getEmail());
        if (customerDTO.getAddress() != null) c.setAddress(customerDTO.getAddress());
        if (customerDTO.getPoints() != null) c.setPoints(customerDTO.getPoints());
        if (customerDTO.getStatus() != null) c.setStatus(customerDTO.getStatus());
        c = customerRepository.save(c);
        return toDTO(c);
    }

    @Override
    public void deleteCustomer(String customerId) {
        Customer c = customerRepository.findByCustomerIdAndStatus(customerId, 1)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        c.setStatus(0);
        customerRepository.save(c);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerDTO> getCustomerByPhone(String phone) {
        return customerRepository.findByPhone(phone).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerDTO> getCustomerByEmail(String email) {
        Optional<Customer> customer = customerRepository.findByEmail(email);
        if (customer.isPresent() && customer.get().getStatus() == 1) {
            return Optional.of(toDTO(customer.get()));
        }
        return Optional.empty();
    }

    @Override
    public void updateCustomerPoints(String customerId, Integer points) {
        Customer c = customerRepository.findByCustomerIdAndStatus(customerId, 1)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        c.setPoints(points);
        customerRepository.save(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDTO> getActiveCustomers() {
        return customerRepository.findByStatus(1).stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public void activateCustomerByEmail(String email) {
        Optional<Customer> customer = customerRepository.findByEmail(email);
        if (customer.isPresent()) {
            Customer c = customer.get();
            if (c.getStatus() != 1) {
                c.setStatus(1);
                customerRepository.save(c);
            }
        }
    }

    private CustomerDTO toDTO(Customer c) {
        CustomerDTO dto = new CustomerDTO();
        dto.setCustomerId(c.getCustomerId());
        dto.setName(c.getName());
        dto.setPhone(c.getPhone());
        dto.setEmail(c.getEmail());
        dto.setAddress(c.getAddress());
        dto.setPoints(c.getPoints());
        dto.setStatus(c.getStatus());
        return dto;
    }

    private Customer toEntity(CustomerDTO dto) {
        Customer c = new Customer();
        // Không set customerId - để JPA tự sinh UUID
        c.setName(dto.getName());
        c.setPhone(dto.getPhone());
        c.setEmail(dto.getEmail());
        c.setAddress(dto.getAddress());
        c.setPoints(dto.getPoints());
        c.setStatus(dto.getStatus());
        return c;
    }
}


