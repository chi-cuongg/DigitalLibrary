package com.example.demo.controller;

import com.example.demo.model.Book;
import com.example.demo.model.User;
import com.example.demo.service.BookService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private BookService bookService;

    @Autowired
    private UserService userService;

    @Autowired
    private com.example.demo.service.GoogleDriveService googleDriveService;

    @GetMapping
    public String dashboard(Model model, @RequestParam(required = false) String success, 
                           @RequestParam(required = false) String error) {
        model.addAttribute("bookCount", bookService.findAll().size());
        model.addAttribute("userCount", userService.findAll().size());
        
        // Check Google Drive status
        boolean driveAvailable = false;
        try {
            driveAvailable = googleDriveService.isDriveAvailable();
        } catch (Exception e) {
            // If Drive service is not available, just set to false
            // This prevents the dashboard from crashing if OAuth is not configured
            driveAvailable = false;
        }
        model.addAttribute("driveAvailable", driveAvailable);
        
        // Add success/error messages
        if (success != null) {
            if ("oauth_success".equals(success)) {
                model.addAttribute("successMessage", "✅ Google Drive đã được kết nối thành công!");
            }
        }
        if (error != null) {
            if ("oauth_error".equals(error)) {
                model.addAttribute("errorMessage", "❌ Lỗi khi kết nối Google Drive. Vui lòng thử lại.");
            } else if ("authorization_failed".equals(error)) {
                model.addAttribute("errorMessage", "❌ Authorization thất bại. Vui lòng thử lại.");
            } else if ("no_code".equals(error)) {
                model.addAttribute("errorMessage", "❌ Không nhận được authorization code. Vui lòng thử lại.");
            } else if ("exchange_failed".equals(error)) {
                model.addAttribute("errorMessage", "❌ Lỗi khi trao đổi authorization code. Vui lòng thử lại.");
            }
        }
        
        return "admin/dashboard";
    }

    // --- Book Management ---
    @GetMapping("/books")
    public String listBooks(Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "5") int size,
                            @RequestParam(required = false) String keyword) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Book> bookPage;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            bookPage = bookService.search(keyword.trim(), pageable);
        } else {
            bookPage = bookService.findAll(pageable);
        }
        
        model.addAttribute("books", bookPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("totalItems", bookPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        return "admin/books/list";
    }

    @GetMapping("/books/add")
    public String showAddBookForm(Model model) {
        model.addAttribute("book", new Book());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/books/form";
    }

    @GetMapping("/books/edit/{id}")
    public String showEditBookForm(@PathVariable Long id, Model model) {
        model.addAttribute("book",
                bookService.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid book Id:" + id)));
        model.addAttribute("categories", categoryService.findAll());
        return "admin/books/form";
    }

    @Autowired
    private com.example.demo.repository.CategoryRepository categoryRepository;

    @PostMapping("/books/save")
    public String saveBook(@ModelAttribute("book") Book book,
            @RequestParam(value = "categoryIds", required = false) List<Long> categoryIds,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {

        if (categoryIds != null) {
            java.util.Set<com.example.demo.model.Category> categories = new java.util.HashSet<>();
            for (Long id : categoryIds) {
                categoryService.findById(id).ifPresent(categories::add);
            }
            book.setCategories(categories);
        }

        bookService.save(book, file);
        return "redirect:/admin/books";
    }

    @GetMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable Long id) {
        bookService.deleteById(id);
        return "redirect:/admin/books";
    }

    // --- User Management ---
    @GetMapping("/users")
    public String listUsers(Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "5") int size,
                            @RequestParam(required = false) String keyword) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<User> userPage;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            userPage = userService.search(keyword.trim(), pageable);
        } else {
            userPage = userService.findAll(pageable);
        }
        
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalItems", userPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        return "admin/users/list";
    }

    @GetMapping("/users/add")
    public String showAddUserForm(Model model) {
        model.addAttribute("user", new User());
        return "admin/users/form";
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        model.addAttribute("user",
                userService.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid user Id:" + id)));
        return "admin/users/form";
    }

    @PostMapping("/users/save")
    public String saveUser(@ModelAttribute User user) {
        userService.save(user);
        return "redirect:/admin/users";
    }

    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return "redirect:/admin/users";
    }

    // --- Category Management ---
    @Autowired
    private com.example.demo.service.CategoryService categoryService;
    
    @Autowired
    private com.example.demo.repository.BookRepository bookRepository;

    @GetMapping("/categories")
    public String listCategories(Model model,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "5") int size,
                                 @RequestParam(required = false) String keyword) {
        // First, handle empty categories deletion (before pagination) - only if no search keyword
        if (keyword == null || keyword.trim().isEmpty()) {
            java.util.List<com.example.demo.model.Category> allCategories = categoryService.findAll();
            java.util.List<Long> categoryIdsToDelete = new java.util.ArrayList<>();
            
            for (com.example.demo.model.Category category : allCategories) {
                java.util.List<com.example.demo.model.Book> books = bookRepository.findByCategoriesId(category.getId());
                if (books.isEmpty()) {
                    categoryIdsToDelete.add(category.getId());
                }
            }
            
            // Delete empty categories
            for (Long categoryId : categoryIdsToDelete) {
                try {
                    categoryService.deleteById(categoryId);
                } catch (Exception e) {
                    System.err.println("Error deleting category " + categoryId + ": " + e.getMessage());
                }
            }
        }
        
        // Now get paginated categories (with search if keyword provided)
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<com.example.demo.model.Category> categoryPage;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            categoryPage = categoryService.search(keyword.trim(), pageable);
        } else {
            categoryPage = categoryService.findAll(pageable);
        }
        
        java.util.List<com.example.demo.model.Category> categories = categoryPage.getContent();
        
        // Count books for each category in current page
        java.util.Map<Long, Long> bookCountMap = new java.util.HashMap<>();
        for (com.example.demo.model.Category category : categories) {
            java.util.List<com.example.demo.model.Book> books = bookRepository.findByCategoriesId(category.getId());
            bookCountMap.put(category.getId(), (long) books.size());
        }
        
        model.addAttribute("categories", categories);
        model.addAttribute("bookCountMap", bookCountMap);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", categoryPage.getTotalPages());
        model.addAttribute("totalItems", categoryPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        return "admin/categories/list";
    }

    @GetMapping("/categories/add")
    public String showAddCategoryForm(Model model) {
        model.addAttribute("category", new com.example.demo.model.Category());
        return "admin/categories/form";
    }

    @GetMapping("/categories/edit/{id}")
    public String showEditCategoryForm(@PathVariable Long id, Model model) {
        model.addAttribute("category",
                categoryService.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid category Id:" + id)));
        return "admin/categories/form";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute com.example.demo.model.Category category) {
        categoryService.save(category);
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        categoryService.deleteById(id);
        return "redirect:/admin/categories";
    }

    // API for Quick Add
    @PostMapping("/categories/api/add")
    @ResponseBody
    public org.springframework.http.ResponseEntity<com.example.demo.model.Category> apiAddCategory(
            @RequestParam String name) {
        com.example.demo.model.Category category = new com.example.demo.model.Category();
        category.setName(name);
        com.example.demo.model.Category savedCategory = categoryService.save(category);
        return org.springframework.http.ResponseEntity.ok(savedCategory);
    }
}
