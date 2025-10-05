package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    
    List<Employee> findByStatus(Integer status);
    
    List<Employee> findByRoleAndStatus(String role, Integer status);
    
    Optional<Employee> findByEmployeeIdAndStatus(String employeeId, Integer status);
    
    Optional<Employee> findByEmail(String email);
    
    @Query("SELECT e FROM Employee e WHERE e.role = 'SHIPPER' AND e.status = 1")
    List<Employee> findActiveShippers();
    
    boolean existsByEmailAndStatus(String email, Integer status);
}
