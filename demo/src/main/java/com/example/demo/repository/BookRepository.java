package com.example.demo.repository;

import com.example.demo.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    @EntityGraph(attributePaths = {"categories", "reviews"})
    java.util.List<Book> findAll();
    
    @EntityGraph(attributePaths = {"categories", "reviews"})
    java.util.List<Book> findByTitleContainingIgnoreCase(String keyword);

    @EntityGraph(attributePaths = {"categories", "reviews"})
    java.util.List<Book> findByCategoriesId(Long categoryId);
    
    @EntityGraph(attributePaths = {"categories"})
    Page<Book> findAll(Pageable pageable);
    
    @EntityGraph(attributePaths = {"categories"})
    Page<Book> findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(
            String title, String author, Pageable pageable);
}
