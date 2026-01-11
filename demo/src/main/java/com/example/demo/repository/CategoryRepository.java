package com.example.demo.repository;

import com.example.demo.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    java.util.Optional<Category> findByName(String name);
    
    org.springframework.data.domain.Page<Category> findByNameContainingIgnoreCase(
            String name, org.springframework.data.domain.Pageable pageable);
}
