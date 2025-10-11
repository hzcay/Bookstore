package com.example.Bookstore.service.impl;

import com.example.Bookstore.dto.EmployeeDTO;
import com.example.Bookstore.entity.Employee;
import com.example.Bookstore.repository.EmployeeRepository;
import com.example.Bookstore.service.EmployeeService;
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
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Override
    public Page<EmployeeDTO> getAllEmployees(String searchTerm, Pageable pageable) {
        Page<Employee> employees;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            employees = employeeRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                searchTerm, searchTerm, pageable);
        } else {
            employees = employeeRepository.findAll(pageable);
        }
        return employees.map(this::convertToDTO);
    }

    @Override
    public Optional<EmployeeDTO> getEmployeeById(String id) {
        return employeeRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public EmployeeDTO createEmployee(EmployeeDTO employeeDTO) {
        Employee employee = convertToEntity(employeeDTO);
        employee.setEmployeeId(null);
        employee.setStatus(0);
        Employee savedEmployee = employeeRepository.save(employee);
        return convertToDTO(savedEmployee);
    }

    @Override
    public EmployeeDTO updateEmployee(String id, EmployeeDTO employeeDTO) {
        Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
        
        employee.setName(employeeDTO.getName());
        employee.setRole(employeeDTO.getRole());
        employee.setPhone(employeeDTO.getPhone());
        employee.setEmail(employeeDTO.getEmail());
        if (employeeDTO.getPassword() != null && !employeeDTO.getPassword().isEmpty()) {
            employee.setPassword(employeeDTO.getPassword());
        }
        employee.setStatus(employeeDTO.getStatus());
        
        Employee updatedEmployee = employeeRepository.save(employee);
        return convertToDTO(updatedEmployee);
    }

    @Override
    public void deleteEmployee(String id) {
        employeeRepository.deleteById(id);
    }

    @Override
    public Optional<EmployeeDTO> getEmployeeByEmail(String email) {
        return employeeRepository.findByEmail(email).map(this::convertToDTO);
    }

    @Override
    public List<EmployeeDTO> getActiveEmployees() {
        return employeeRepository.findByStatus(1).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public void activateEmployeeByEmail(String email) {
        Optional<Employee> employee = employeeRepository.findByEmail(email);
        if (employee.isPresent()) {
            Employee e = employee.get();
            if (e.getStatus() != 1) {
                e.setStatus(1);
                employeeRepository.save(e);
            }
        }
    }

    private EmployeeDTO convertToDTO(Employee employee) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setName(employee.getName());
        dto.setRole(employee.getRole());
        dto.setPhone(employee.getPhone());
        dto.setEmail(employee.getEmail());
        dto.setStatus(employee.getStatus());
        return dto;
    }

    private Employee convertToEntity(EmployeeDTO dto) {
        Employee employee = new Employee();
        employee.setEmployeeId(dto.getEmployeeId());
        employee.setName(dto.getName());
        employee.setRole(dto.getRole());
        employee.setPhone(dto.getPhone());
        employee.setEmail(dto.getEmail());
        employee.setPassword(dto.getPassword());
        employee.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        return employee;
    }
}

