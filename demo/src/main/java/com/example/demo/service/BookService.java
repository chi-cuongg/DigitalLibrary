package com.example.demo.service;

import com.example.demo.model.Book;
import com.example.demo.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public org.springframework.data.domain.Page<Book> findAll(org.springframework.data.domain.Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    public org.springframework.data.domain.Page<Book> search(String keyword, org.springframework.data.domain.Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return bookRepository.findAll(pageable);
        }
        return bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(keyword, keyword, pageable);
    }

    public Optional<Book> findById(Long id) {
        return bookRepository.findById(id);
    }

    public Book save(Book book) {
        return bookRepository.save(book);
    }

    public Book save(Book book, org.springframework.web.multipart.MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            String storedFileName = fileStorageService.storeFile(file);

            com.example.demo.model.BookFile bookFile = new com.example.demo.model.BookFile();
            bookFile.setFileName(file.getOriginalFilename());
            bookFile.setFilePath(storedFileName);
            bookFile.setFileType(file.getContentType());
            bookFile.setFileSize(file.getSize());
            bookFile.setBook(book);

            book.getFiles().add(bookFile);
        }
        return bookRepository.save(book);
    }

    public void deleteById(Long id) {
        bookRepository.deleteById(id);
    }
}
