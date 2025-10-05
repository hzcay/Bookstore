package com.example.Bookstore.repository;

import com.example.Bookstore.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, String> {
    
    List<Publisher> findByStatus(Integer status);
    
    Optional<Publisher> findByPublisherIdAndStatus(String publisherId, Integer status);
    
    boolean existsByNameAndStatus(String name, Integer status);
}
