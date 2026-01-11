package com.example.demo.controller;

import com.example.demo.service.GoogleDriveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OAuthController {

    private static final Logger logger = LoggerFactory.getLogger(OAuthController.class);

    @Autowired
    private GoogleDriveService googleDriveService;

    @GetMapping("/oauth2/authorize")
    public String authorize() {
        try {
            String authorizationUrl = googleDriveService.getAuthorizationUrl();
            logger.info("Redirecting to OAuth authorization URL: {}", authorizationUrl);
            return "redirect:" + authorizationUrl;
        } catch (Exception e) {
            logger.error("Error getting authorization URL: {}", e.getMessage(), e);
            return "redirect:/admin?error=oauth_error";
        }
    }

    @GetMapping("/oauth2/callback")
    public String callback(@RequestParam(required = false) String code,
                          @RequestParam(required = false) String error,
                          Model model) {
        if (error != null) {
            logger.error("OAuth error: {}", error);
            model.addAttribute("error", "Authorization failed: " + error);
            return "redirect:/admin?error=authorization_failed";
        }

        if (code == null || code.isEmpty()) {
            logger.error("No authorization code received");
            model.addAttribute("error", "No authorization code received");
            return "redirect:/admin?error=no_code";
        }

        try {
            logger.info("Received authorization code, exchanging for tokens...");
            googleDriveService.exchangeCodeForTokens(code);
            logger.info("✅ OAuth authorization completed successfully!");
            return "redirect:/admin?success=oauth_success";
        } catch (Exception e) {
            logger.error("Error exchanging code for tokens: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to exchange authorization code: " + e.getMessage());
            return "redirect:/admin?error=exchange_failed";
        }
    }
}
