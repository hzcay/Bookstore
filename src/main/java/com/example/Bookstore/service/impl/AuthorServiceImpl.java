package com.example.Bookstore.service.impl;

import com.example.Bookstore.dto.AuthorDTO;
import com.example.Bookstore.entity.Author;
import com.example.Bookstore.repository.AuthorRepository;
import com.example.Bookstore.service.AuthorService;
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
public class AuthorServiceImpl implements AuthorService {
    
    @Autowired
    private AuthorRepository authorRepository;
    
    @Override
    public Page<AuthorDTO> getAllAuthors(String search, Pageable pageable) {
        Page<Author> authors;
        if (search != null && !search.trim().isEmpty()) {
            authors = authorRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            authors = authorRepository.findAll(pageable);
        }
        return authors.map(this::convertToDTO);
    }
    
    @Override
    public Optional<AuthorDTO> getAuthorById(String id) {
        return authorRepository.findById(id).map(this::convertToDTO);
    }
    
    @Override
    public AuthorDTO createAuthor(AuthorDTO authorDTO) {
        Author author = new Author();
        author.setName(authorDTO.getName());
        author.setDescription(authorDTO.getDescription());
        author.setStatus(1);
        Author saved = authorRepository.save(author);
        return convertToDTO(saved);
    }
    
    @Override
    public AuthorDTO updateAuthor(String id, AuthorDTO authorDTO) {
        Author author = authorRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Author not found"));
        
        author.setName(authorDTO.getName());
        author.setDescription(authorDTO.getDescription());
        if (authorDTO.getStatus() != null) {
            author.setStatus(authorDTO.getStatus());
        }
        
        Author updated = authorRepository.save(author);
        return convertToDTO(updated);
    }
    
    @Override
    public void deleteAuthor(String id) {
        Author author = authorRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Author not found"));
        author.setStatus(0);
        authorRepository.save(author);
    }
    
    @Override
    public List<AuthorDTO> getActiveAuthors() {
        return authorRepository.findByStatus(1).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    private AuthorDTO convertToDTO(Author author) {
        AuthorDTO dto = new AuthorDTO();
        dto.setAuthorId(author.getAuthorId());
        dto.setName(author.getName());
        dto.setDescription(author.getDescription());
        dto.setStatus(author.getStatus());
        return dto;
    }
    
    private Author convertToEntity(AuthorDTO dto) {
        Author author = new Author();
        author.setAuthorId(dto.getAuthorId());
        author.setName(dto.getName());
        author.setDescription(dto.getDescription());
        author.setStatus(dto.getStatus());
        return author;
    }
}

