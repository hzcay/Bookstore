package com.example.Bookstore.service;

import com.example.Bookstore.dto.EmployeeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface EmployeeService {
    Page<EmployeeDTO> getAllEmployees(String searchTerm, Pageable pageable);
    Optional<EmployeeDTO> getEmployeeById(String id);
    EmployeeDTO createEmployee(EmployeeDTO employeeDTO);
    EmployeeDTO updateEmployee(String id, EmployeeDTO employeeDTO);
    void deleteEmployee(String id);
    Optional<EmployeeDTO> getEmployeeByEmail(String email);
    List<EmployeeDTO> getActiveEmployees();
    void activateEmployeeByEmail(String email);
}

