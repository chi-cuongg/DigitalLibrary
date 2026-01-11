package com.example.demo.controller;

import com.example.demo.service.GoogleDriveService;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.service.CrawlerService;

@Controller
@RequestMapping("/admin/crawler")
public class CrawlerController {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerController.class);

    @Autowired
    private CrawlerService crawlerService;
    
    @Autowired
    private GoogleDriveService googleDriveService;

    /**
     * Display crawler page
     */
    @GetMapping
    public String crawlerPage(Model model) {
        logger.info("CrawlerController: crawlerPage method called");
        
        // Check Google Drive status
        boolean driveAvailable = googleDriveService.isDriveAvailable();
        model.addAttribute("driveAvailable", driveAvailable);
        
        return "admin/crawler";
    }
    
    /**
     * Test endpoint to check Google Drive status
     */
    @GetMapping("/test-drive")
    @ResponseBody
    public Map<String, Object> testDrive() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean available = googleDriveService.isDriveAvailable();
            response.put("available", available);
            response.put("status", available ? "✅ Google Drive is ready!" : "❌ Google Drive is not available");
            response.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            response.put("available", false);
            response.put("status", "❌ Error: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
        }
        return response;
    }

    /**
     * API endpoint to start crawling
     */
    @PostMapping("/crawl")
    @ResponseBody
    public Map<String, Object> crawl(@RequestParam String url,
                                      @RequestParam(defaultValue = "1") int maxBooks,
                                      @RequestParam(defaultValue = "true") boolean downloadFiles) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            CrawlerService.CrawlResult result = crawlerService.crawlBooksFromUrl(url, maxBooks, downloadFiles);
            
            response.put("success", true);
            response.put("message", "Crawl completed successfully");
            response.put("result", Map.of(
                "successCount", result.getSuccessCount(),
                "failedCount", result.getFailedCount(),
                "skippedCount", result.getSkippedCount(),
                "duration", result.getDuration(),
                "errors", result.getErrors()
            ));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error during crawl: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
        }
        
        return response;
    }

    /**
     * Crawl from form submission (redirect)
     */
    @PostMapping("/start")
    public String startCrawl(@RequestParam String url,
                            @RequestParam(defaultValue = "1") int maxBooks,
                            @RequestParam(defaultValue = "true") boolean downloadFiles,
                            RedirectAttributes redirectAttributes) {
        try {
            CrawlerService.CrawlResult result = crawlerService.crawlBooksFromUrl(url, maxBooks, downloadFiles);
            
            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("message", 
                String.format("Đã crawl thành công %d cuốn sách! (Thất bại: %d, Bỏ qua: %d)", 
                    result.getSuccessCount(), result.getFailedCount(), result.getSkippedCount()));
            redirectAttributes.addFlashAttribute("result", result);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("success", false);
            redirectAttributes.addFlashAttribute("message", "Lỗi khi crawl: " + e.getMessage());
        }
        
        return "redirect:/admin/crawler";
    }
}