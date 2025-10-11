package com.example.Bookstore.service.impl;

import com.example.Bookstore.dto.CategoryDTO;
import com.example.Bookstore.entity.Category;
import com.example.Bookstore.repository.CategoryRepository;
import com.example.Bookstore.service.CategoryService;
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
public class CategoryServiceImpl implements CategoryService {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Override
    public Page<CategoryDTO> getAllCategories(String search, Pageable pageable) {
        Page<Category> categories;
        if (search != null && !search.trim().isEmpty()) {
            categories = categoryRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            categories = categoryRepository.findAll(pageable);
        }
        return categories.map(this::convertToDTO);
    }
    
    @Override
    public Optional<CategoryDTO> getCategoryById(String id) {
        return categoryRepository.findById(id).map(this::convertToDTO);
    }
    
    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setStatus(1);
        Category saved = categoryRepository.save(category);
        return convertToDTO(saved);
    }
    
    @Override
    public CategoryDTO updateCategory(String id, CategoryDTO categoryDTO) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        
        category.setName(categoryDTO.getName());
        if (categoryDTO.getStatus() != null) {
            category.setStatus(categoryDTO.getStatus());
        }
        
        Category updated = categoryRepository.save(category);
        return convertToDTO(updated);
    }
    
    @Override
    public void deleteCategory(String id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Category not found"));
        category.setStatus(0);
        categoryRepository.save(category);
    }
    
    @Override
    public List<CategoryDTO> getActiveCategories() {
        return categoryRepository.findByStatus(1).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    private CategoryDTO convertToDTO(Category category) {
        CategoryDTO dto = new CategoryDTO();
        dto.setCategoryId(category.getCategoryId());
        dto.setName(category.getName());
        dto.setStatus(category.getStatus());
        return dto;
    }
    
    private Category convertToEntity(CategoryDTO dto) {
        Category category = new Category();
        category.setCategoryId(dto.getCategoryId());
        category.setName(dto.getName());
        category.setStatus(dto.getStatus());
        return category;
    }
}

