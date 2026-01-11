package com.example.demo.service;

import com.example.demo.model.Book;
import com.example.demo.model.BookFile;
import com.example.demo.repository.BookRepository;
import com.google.api.services.drive.model.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookService.class);

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private GoogleDriveService googleDriveService;

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

    @Transactional
    public Book save(Book book, org.springframework.web.multipart.MultipartFile file) {
        if (file != null && !file.isEmpty()) {
            try {
                String fileName = file.getOriginalFilename();
                String fileType = file.getContentType();
                Long fileSize = file.getSize();
                String filePath = null;
                String displayFileName = fileName;

                // Check if Google Drive is available
                boolean driveAvailable = googleDriveService.isDriveAvailable();
                logger.info("🔍 Google Drive availability check: {}", 
                        driveAvailable ? "✅ AVAILABLE" : "❌ NOT AVAILABLE");

                if (driveAvailable) {
                    // Try to upload to Google Drive first
                    try {
                        logger.info("🚀 Uploading file to Google Drive: {}", fileName);
                        String driveFileId = googleDriveService.uploadFile(
                                file.getInputStream(), 
                                fileName, 
                                fileType != null ? fileType : "application/pdf"
                        );
                        
                        logger.info("✅✅✅ File uploaded to Google Drive successfully! ✅✅✅");
                        logger.info("📎 Drive File ID: {}", driveFileId);

                        // Get file metadata from Drive
                        File driveFile = googleDriveService.getFileMetadata(driveFileId);
                        fileSize = driveFile.getSize() != null ? driveFile.getSize() : fileSize;
                        fileType = driveFile.getMimeType() != null ? driveFile.getMimeType() : fileType;
                        
                        // Store Drive file ID as filePath
                        filePath = driveFileId;
                        displayFileName = driveFile.getName();
                        
                        logger.info("✅ File saved to Google Drive! File ID: {}", filePath);
                    } catch (Exception driveEx) {
                        logger.error("❌❌❌ Google Drive upload FAILED! ❌❌❌");
                        logger.error("Error message: {}", driveEx.getMessage());
                        // Fallback to local storage
                        logger.warn("🔄 Falling back to local storage...");
                        String storedFileName = fileStorageService.storeFile(file);
                        filePath = storedFileName;
                        logger.info("✅ File saved to local storage: {}", filePath);
                    }
                } else {
                    // Drive not available, use local storage
                    logger.warn("⚠️ Google Drive is not available, using local storage");
                    String storedFileName = fileStorageService.storeFile(file);
                    filePath = storedFileName;
                    logger.info("✅ File saved to local storage: {}", filePath);
                }

                // Create and add BookFile
                BookFile bookFile = new BookFile();
                bookFile.setFileName(displayFileName);
                bookFile.setFilePath(filePath);
                bookFile.setFileType(fileType);
                bookFile.setFileSize(fileSize);
                bookFile.setBook(book);

                book.getFiles().add(bookFile);
            } catch (Exception e) {
                logger.error("❌ Error processing file: {}", e.getMessage(), e);
                throw new RuntimeException("Could not process file: " + e.getMessage(), e);
            }
        }
        return bookRepository.save(book);
    }

    public void deleteById(Long id) {
        bookRepository.deleteById(id);
    }
}
