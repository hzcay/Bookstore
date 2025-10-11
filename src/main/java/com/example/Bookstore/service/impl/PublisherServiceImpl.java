package com.example.Bookstore.service.impl;

import com.example.Bookstore.dto.PublisherDTO;
import com.example.Bookstore.entity.Publisher;
import com.example.Bookstore.repository.PublisherRepository;
import com.example.Bookstore.service.PublisherService;
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
public class PublisherServiceImpl implements PublisherService {
    
    @Autowired
    private PublisherRepository publisherRepository;
    
    @Override
    public Page<PublisherDTO> getAllPublishers(String search, Pageable pageable) {
        Page<Publisher> publishers;
        if (search != null && !search.trim().isEmpty()) {
            publishers = publisherRepository.findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(search, search, pageable);
        } else {
            publishers = publisherRepository.findAll(pageable);
        }
        return publishers.map(this::convertToDTO);
    }
    
    @Override
    public Optional<PublisherDTO> getPublisherById(String id) {
        return publisherRepository.findById(id).map(this::convertToDTO);
    }
    
    @Override
    public PublisherDTO createPublisher(PublisherDTO publisherDTO) {
        Publisher publisher = new Publisher();
        publisher.setName(publisherDTO.getName());
        publisher.setAddress(publisherDTO.getAddress());
        publisher.setPhone(publisherDTO.getPhone());
        publisher.setStatus(1);
        Publisher saved = publisherRepository.save(publisher);
        return convertToDTO(saved);
    }
    
    @Override
    public PublisherDTO updatePublisher(String id, PublisherDTO publisherDTO) {
        Publisher publisher = publisherRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Publisher not found"));
        
        publisher.setName(publisherDTO.getName());
        publisher.setAddress(publisherDTO.getAddress());
        publisher.setPhone(publisherDTO.getPhone());
        if (publisherDTO.getStatus() != null) {
            publisher.setStatus(publisherDTO.getStatus());
        }
        
        Publisher updated = publisherRepository.save(publisher);
        return convertToDTO(updated);
    }
    
    @Override
    public void deletePublisher(String id) {
        Publisher publisher = publisherRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Publisher not found"));
        publisher.setStatus(0);
        publisherRepository.save(publisher);
    }
    
    @Override
    public List<PublisherDTO> getActivePublishers() {
        return publisherRepository.findByStatus(1).stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    private PublisherDTO convertToDTO(Publisher publisher) {
        PublisherDTO dto = new PublisherDTO();
        dto.setPublisherId(publisher.getPublisherId());
        dto.setName(publisher.getName());
        dto.setAddress(publisher.getAddress());
        dto.setPhone(publisher.getPhone());
        dto.setStatus(publisher.getStatus());
        return dto;
    }
    
    private Publisher convertToEntity(PublisherDTO dto) {
        Publisher publisher = new Publisher();
        publisher.setPublisherId(dto.getPublisherId());
        publisher.setName(dto.getName());
        publisher.setAddress(dto.getAddress());
        publisher.setPhone(dto.getPhone());
        publisher.setStatus(dto.getStatus());
        return publisher;
    }
}

