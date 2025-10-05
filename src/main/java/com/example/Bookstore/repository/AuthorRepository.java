package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, String> {
    
    List<Author> findByStatus(Integer status);
    
    Optional<Author> findByAuthorIdAndStatus(String authorId, Integer status);
    
    boolean existsByNameAndStatus(String name, Integer status);
}
