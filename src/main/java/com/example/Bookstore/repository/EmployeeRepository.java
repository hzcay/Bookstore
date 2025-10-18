package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    
    List<Employee> findByStatus(Integer status);
    
    List<Employee> findByRoleAndStatus(String role, Integer status);
    
    Page<Employee> findByRoleAndStatus(String role, Integer status, Pageable pageable);
    
    @Query("SELECT e FROM Employee e WHERE " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "e.role = :role AND e.status = :status")
    Page<Employee> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndRoleAndStatus(
        String name, String email, String role, Integer status, Pageable pageable);
    
    Optional<Employee> findByEmployeeIdAndStatus(String employeeId, Integer status);
    
    Optional<Employee> findByEmail(String email);
    
    @Query("SELECT e FROM Employee e WHERE " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.phone) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Employee> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String search, Pageable pageable);
    
    Page<Employee> findByRole(String role, Pageable pageable);
    
    @Query("SELECT e FROM Employee e WHERE " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.phone) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "e.role = :role")
    Page<Employee> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseAndRole(
        String search, String role, Pageable pageable);
    
    @Query("SELECT e FROM Employee e WHERE e.role = 'SHIPPER' AND e.status = 1")
    List<Employee> findActiveShippers();
    
    boolean existsByEmailAndStatus(String email, Integer status);
}
