package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    
    List<Category> findByStatus(Integer status);
    
    Optional<Category> findByCategoryIdAndStatus(String categoryId, Integer status);
    
    boolean existsByNameAndStatus(String name, Integer status);
    
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
