package com.example.demo.controller;

import com.example.demo.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @Autowired
    private com.example.demo.service.BookService bookService;

    @Autowired
    private com.example.demo.service.CategoryService categoryService;

    @Autowired
    private com.example.demo.repository.BookRepository bookRepository;

    @Autowired
    private com.example.demo.service.UserService userService;

    @GetMapping("/")
    public String index(Model model) {
        // Need categories for the header dropdown
        model.addAttribute("categories", categoryService.findAll());
        return "index";
    }

    @GetMapping("/catalog")
    public String catalog(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            Model model) {

        java.util.List<com.example.demo.model.Book> books;

        if (keyword != null && !keyword.isEmpty()) {
            books = bookRepository.findByTitleContainingIgnoreCase(keyword);
        } else if (categoryId != null) {
            books = bookRepository.findByCategoriesId(categoryId);
        } else {
            books = bookService.findAll();
        }

        model.addAttribute("books", books);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("currentCategoryId", categoryId);
        model.addAttribute("currentKeyword", keyword);

        return "catalog";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, Model model, HttpSession session) {
        // Simple manual authentication for demo purposes
        // In a real app with Spring Security, this would be handled automatically
        var users = userService.findAll(); // In real app use findByEmail

        for (User user : users) {
            // Basic check - in real app password should be hashed
            if (user.getEmail() != null && user.getEmail().equals(email) && user.getPassword().equals(password)) {
                session.setAttribute("user", user);
                // Check if user has ADMIN role
                boolean isAdmin = user.getRoles().stream()
                        .anyMatch(role -> role.getName().equalsIgnoreCase("ADMIN")
                                || role.getName().equalsIgnoreCase("ROLE_ADMIN"));

                if (isAdmin) {
                    return "redirect:/admin";
                } else {
                    return "redirect:/";
                }
            }
        }

        model.addAttribute("error", "Email hoặc mật khẩu không đúng!");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("user");
        return "redirect:/";
    }

    @GetMapping("/books/{id}")
    public String bookDetails(@PathVariable Long id, Model model, HttpSession session) {
        com.example.demo.model.Book book = bookService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid book Id:" + id));
        
        // Increment view count
        Long currentViewCount = book.getViewCount() != null ? book.getViewCount() : 0L;
        book.setViewCount(currentViewCount + 1);
        bookService.save(book);
        
        model.addAttribute("book", book);
        model.addAttribute("categories", categoryService.findAll()); // For header

        User currentUser = (User) session.getAttribute("user");
        if (currentUser != null) {
            com.example.demo.model.Review userReview = reviewService.findByBookAndUser(id, currentUser.getId());
            model.addAttribute("userReview", userReview);
        }

        return "book-details";
    }

    @Autowired
    private com.example.demo.service.FileStorageService fileStorageService;

    @Autowired
    private com.example.demo.service.GoogleDriveService googleDriveService;

    @Autowired
    private com.example.demo.repository.BookFileRepository bookFileRepository;

    @GetMapping("/books/download/{fileId}")
    public org.springframework.http.ResponseEntity<?> downloadFile(
            @org.springframework.web.bind.annotation.PathVariable Long fileId) {
        com.example.demo.model.BookFile bookFile = bookFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with id " + fileId));

        String filePath = bookFile.getFilePath();
        
        // Check if filePath is a Google Drive file ID (typically doesn't contain path separators)
        // Drive file IDs are usually alphanumeric strings of length 25-50
        boolean isDriveFile = filePath != null && !filePath.contains("/") && !filePath.contains("\\") 
                && filePath.length() > 20 && filePath.length() < 100;
        
        if (isDriveFile) {
            try {
                // Get download URL from Google Drive
                String downloadUrl = googleDriveService.getDownloadUrl(filePath);
                
                // Redirect to Google Drive download URL
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                        .header(org.springframework.http.HttpHeaders.LOCATION, downloadUrl)
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Failed to get download URL from Google Drive: " + e.getMessage(), e);
            }
        } else {
            // Use local file storage (backward compatibility)
            org.springframework.core.io.Resource resource = fileStorageService.loadFileAsResource(filePath);
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(bookFile.getFileType()))
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + bookFile.getFileName() + "\"")
                    .body(resource);
        }
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @Autowired
    private com.example.demo.repository.RoleRepository roleRepository;

    @PostMapping("/register")
    public String register(@RequestParam String fullName, @RequestParam String email,
            @RequestParam String password, @RequestParam String confirmPassword, Model model) {

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "register";
        }

        // Check if user already exists
        if (userService.findByEmail(email) != null) {
            model.addAttribute("error", "Email này đã được đăng ký!");
            return "register";
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setUsername(email); // Use email as username
        user.setPassword(password); // In real app: passwordEncoder.encode(password)
        user.setEnabled(true);

        com.example.demo.model.Role userRole = roleRepository.findByName("USER");
        if (userRole == null) {
            // Create default role if not exists
            userRole = new com.example.demo.model.Role();
            userRole.setName("USER");
            roleRepository.save(userRole);
        }

        java.util.Set<com.example.demo.model.Role> roles = new java.util.HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        userService.save(user);

        return "redirect:/login?registered";
    }

    @Autowired
    private com.example.demo.service.ReviewService reviewService;

    @PostMapping("/books/{id}/review")
    public String addReview(@org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestParam Integer rating,
            @RequestParam String content,
            HttpSession session) {

        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }

        com.example.demo.model.Book book = bookService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid book Id:" + id));

        // Check for existing review
        com.example.demo.model.Review review = reviewService.findByBookAndUser(id, currentUser.getId());

        if (review == null) {
            review = new com.example.demo.model.Review();
            review.setBook(book);
            review.setUser(currentUser);
        } else {
            review.setUpdatedAt(java.time.LocalDateTime.now());
        }

        review.setRating(rating);
        review.setComment(content);

        reviewService.save(review);

        return "redirect:/books/" + id;
    }
}
