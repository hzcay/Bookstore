package com.example.Bookstore.service;

import com.example.Bookstore.dto.PublisherDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PublisherService {
    
    Page<PublisherDTO> getAllPublishers(String search, Pageable pageable);
    
    Optional<PublisherDTO> getPublisherById(String id);
    
    PublisherDTO createPublisher(PublisherDTO publisherDTO);
    
    PublisherDTO updatePublisher(String id, PublisherDTO publisherDTO);
    
    void deletePublisher(String id);
    
    List<PublisherDTO> getActivePublishers();
}

