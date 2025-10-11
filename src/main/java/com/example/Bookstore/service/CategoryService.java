package com.example.Bookstore.service;

import com.example.Bookstore.dto.CategoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CategoryService {
    
    Page<CategoryDTO> getAllCategories(String search, Pageable pageable);
    
    Optional<CategoryDTO> getCategoryById(String id);
    
    CategoryDTO createCategory(CategoryDTO categoryDTO);
    
    CategoryDTO updateCategory(String id, CategoryDTO categoryDTO);
    
    void deleteCategory(String id);
    
    List<CategoryDTO> getActiveCategories();
}

