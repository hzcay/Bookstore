package com.example.Bookstore.service;

import com.example.Bookstore.dto.AuthorDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AuthorService {
    
    Page<AuthorDTO> getAllAuthors(String search, Pageable pageable);
    
    Optional<AuthorDTO> getAuthorById(String id);
    
    AuthorDTO createAuthor(AuthorDTO authorDTO);
    
    AuthorDTO updateAuthor(String id, AuthorDTO authorDTO);
    
    void deleteAuthor(String id);
    
    List<AuthorDTO> getActiveAuthors();
}

