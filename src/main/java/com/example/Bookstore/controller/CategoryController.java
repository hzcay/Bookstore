package com.example.Bookstore.controller;

import com.example.Bookstore.entity.Category;
import com.example.Bookstore.repository.CategoryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryRepository categoryRepo;

    public CategoryController(CategoryRepository categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    @GetMapping
    public List<CategoryDTO> list(@RequestParam(required = false) Integer status) {
        List<Category> cats = (status == null)
                ? categoryRepo.findAll()
                : categoryRepo.findByStatus(status);

        return cats.stream()
                .sorted(Comparator.comparing(
                        c -> c.getName() == null ? "" : c.getName(),
                        String.CASE_INSENSITIVE_ORDER
                ))
                .map(c -> new CategoryDTO(c.getCategoryId(), c.getName()))
                .toList();
    }

    @GetMapping("/{id}")
    public CategoryDTO one(@PathVariable("id") String categoryId,
                           @RequestParam(required = false) Integer status) {
        Category c = (status == null)
                ? categoryRepo.findById(categoryId).orElseThrow()
                : categoryRepo.findByCategoryIdAndStatus(categoryId, status).orElseThrow();
        return new CategoryDTO(c.getCategoryId(), c.getName());
    }

    public record CategoryDTO(String categoryId, String name) {}
}

