package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService() {
        this.fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // Normalize file name
        String originalFileName = org.springframework.util.StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Check if the file's name contains invalid characters
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFileName);
            }

            // Generate a unique file name to avoid collisions
            String fileName = UUID.randomUUID().toString() + "_" + originalFileName;

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }

    public org.springframework.core.io.Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(
                    filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (java.net.MalformedURLException ex) {
            throw new RuntimeException("File not found " + fileName, ex);
        }
    }

    /**
     * Download file from URL and save to uploads directory
     * @param fileUrl The URL of the file to download
     * @param fileName The name to save the file as (optional, will use URL filename if null)
     * @return The saved filename
     */
    public String downloadFileFromUrl(String fileUrl, String fileName) throws IOException {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FileStorageService.class);
        
        try {
            // Handle Google Drive links - convert to direct download format
            String downloadUrl = fileUrl;
            if (fileUrl != null && (fileUrl.contains("docs.google.com") || fileUrl.contains("drive.google.com"))) {
                logger.info("Detected Google Drive link: {}", fileUrl);
                
                // Convert Google Drive view link to direct download
                // Format: https://docs.google.com/uc?id=FILE_ID
                if (fileUrl.contains("uc?id=")) {
                    // Already in uc format, add export=download
                    if (!fileUrl.contains("export=download")) {
                        downloadUrl = fileUrl + (fileUrl.contains("?") ? "&" : "?") + "export=download";
                    }
                } else if (fileUrl.contains("/file/d/")) {
                    // Extract file ID from /file/d/FILE_ID/... format
                    int start = fileUrl.indexOf("/file/d/") + 8;
                    int end = fileUrl.indexOf("/", start);
                    if (end == -1) {
                        // Try to find ? or & as end marker
                        int qMark = fileUrl.indexOf("?", start);
                        int aMark = fileUrl.indexOf("&", start);
                        end = fileUrl.length();
                        if (qMark != -1) end = Math.min(end, qMark);
                        if (aMark != -1) end = Math.min(end, aMark);
                    }
                    String fileId = fileUrl.substring(start, end);
                    downloadUrl = "https://docs.google.com/uc?id=" + fileId + "&export=download";
                    logger.info("Converted to direct download URL: {}", downloadUrl);
                } else if (fileUrl.contains("/open?id=")) {
                    // Extract from /open?id=FILE_ID format
                    int start = fileUrl.indexOf("/open?id=") + 9;
                    int end = fileUrl.indexOf("&", start);
                    if (end == -1) end = fileUrl.length();
                    String fileId = fileUrl.substring(start, end);
                    downloadUrl = "https://docs.google.com/uc?id=" + fileId + "&export=download";
                    logger.info("Converted to direct download URL: {}", downloadUrl);
                }
            }
            
            java.net.URI uri = java.net.URI.create(downloadUrl);
            java.net.URL url = uri.toURL();
            
            logger.info("Downloading from URL: {}", downloadUrl);
            
            // Create connection with proper settings
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setRequestProperty("Accept", "*/*");
            connection.setInstanceFollowRedirects(true); // Enable redirect following
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(60000);
            
            // Handle redirects manually if needed (for Google Drive)
            int responseCode = connection.getResponseCode();
            logger.info("Response code: {}", responseCode);
            
            // Follow redirects (Google Drive may redirect multiple times)
            int redirectCount = 0;
            while (responseCode == java.net.HttpURLConnection.HTTP_MOVED_PERM || 
                   responseCode == java.net.HttpURLConnection.HTTP_MOVED_TEMP ||
                   responseCode == java.net.HttpURLConnection.HTTP_SEE_OTHER) {
                if (redirectCount++ > 5) {
                    throw new IOException("Too many redirects");
                }
                String location = connection.getHeaderField("Location");
                if (location == null) break;
                
                logger.info("Following redirect to: {}", location);
                connection.disconnect();
                
                uri = java.net.URI.create(location);
                url = uri.toURL();
                connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                connection.setRequestProperty("Accept", "*/*");
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                
                responseCode = connection.getResponseCode();
                logger.info("Redirect response code: {}", responseCode);
            }
            
            // Check if we got a successful response
            if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
                // Try to read error stream
                java.io.InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    byte[] errorBytes = errorStream.readAllBytes();
                    String errorMessage = new String(errorBytes);
                    logger.error("HTTP error response: {}", errorMessage.substring(0, Math.min(500, errorMessage.length())));
                }
                throw new IOException("HTTP error code: " + responseCode);
            }
            
            // Get content type to verify it's actually a file
            String contentType = connection.getContentType();
            logger.info("Content-Type: {}", contentType);
            
            // Verify it's a PDF file (reject other formats)
            if (contentType != null) {
                String lowerContentType = contentType.toLowerCase();
                if (lowerContentType.contains("epub") || lowerContentType.contains("mobi") ||
                    lowerContentType.contains("azw") || lowerContentType.contains("kindle") ||
                    lowerContentType.contains("audio") || lowerContentType.contains("mp3")) {
                    throw new IOException("File is not PDF format. Content-Type: " + contentType + ". Only PDF files are accepted.");
                }
                
                // Check if it's HTML (might be Google Drive warning page)
                if (lowerContentType.startsWith("text/html")) {
                    // Read first few bytes to check
                    try (java.io.InputStream testIn = connection.getInputStream()) {
                        byte[] buffer = new byte[1024];
                        int bytesRead = testIn.read(buffer);
                        String contentStart = new String(buffer, 0, Math.min(bytesRead, 500));
                        if (contentStart.contains("<html") || contentStart.contains("<!DOCTYPE")) {
                            throw new IOException("Received HTML instead of file. The file may be too large or require permission. Content preview: " + contentStart.substring(0, Math.min(200, contentStart.length())));
                        }
                    }
                }
            }
            
            // Get filename - prioritize Content-Disposition header for Google Drive
            String originalFileName = fileName;
            if (originalFileName == null || originalFileName.isEmpty() || 
                originalFileName.equals("uc") || originalFileName.length() < 3) {
                
                // Try to get filename from Content-Disposition header first
                String contentDisposition = connection.getHeaderField("Content-Disposition");
                logger.info("Content-Disposition header: {}", contentDisposition);
                
                if (contentDisposition != null && contentDisposition.contains("filename")) {
                    try {
                        // Extract filename from Content-Disposition header
                        int filenameIndex = contentDisposition.indexOf("filename");
                        int start = contentDisposition.indexOf("=", filenameIndex) + 1;
                        if (start > 0) {
                            String filenamePart = contentDisposition.substring(start).trim();
                            if (filenamePart.startsWith("\"") || filenamePart.startsWith("'")) {
                                char quote = filenamePart.charAt(0);
                                int end = filenamePart.indexOf(quote, 1);
                                if (end > 0) {
                                    originalFileName = filenamePart.substring(1, end);
                                }
                            } else {
                                int end = filenamePart.indexOf(";");
                                if (end > 0) {
                                    originalFileName = filenamePart.substring(0, end).trim();
                                } else {
                                    originalFileName = filenamePart.trim();
                                }
                            }
                            // Decode URL encoding if any
                            if (originalFileName != null && !originalFileName.isEmpty()) {
                                originalFileName = java.net.URLDecoder.decode(originalFileName, "UTF-8");
                                logger.info("Extracted filename from Content-Disposition: {}", originalFileName);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error parsing Content-Disposition header: {}", e.getMessage());
                    }
                }
                
                // If still no filename, try from URL or use default
                if (originalFileName == null || originalFileName.isEmpty() || 
                    originalFileName.equals("uc") || originalFileName.length() < 3) {
                    
                    // For Google Drive, don't try to extract from URL path
                    if (fileUrl != null && (fileUrl.contains("docs.google.com") || fileUrl.contains("drive.google.com"))) {
                        // Always use PDF as default for Google Drive files
                        originalFileName = "book.pdf";
                        logger.info("Using default filename for Google Drive: {}", originalFileName);
                    } else {
                        // Try from URL path
                        String path = url.getPath();
                        if (path != null && path.contains(".") && path.length() > path.lastIndexOf('/') + 1) {
                            String urlFileName = path.substring(path.lastIndexOf('/') + 1);
                            // Validate filename
                            if (urlFileName.length() > 3 && !urlFileName.equals("uc") && urlFileName.contains(".")) {
                                originalFileName = urlFileName;
                                logger.info("Extracted filename from URL path: {}", originalFileName);
                            }
                        }
                        
                        // Final fallback - always use PDF
                        if (originalFileName == null || originalFileName.isEmpty() || 
                            originalFileName.equals("uc") || originalFileName.length() < 3) {
                            originalFileName = "download.pdf";
                            logger.info("Using default fallback filename: {}", originalFileName);
                        }
                    }
                }
            }
            
            logger.info("Saving file as: {}", originalFileName);

            // Clean filename
            originalFileName = org.springframework.util.StringUtils.cleanPath(originalFileName);
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Sorry! Filename contains invalid path sequence " + originalFileName);
            }

            // Generate unique filename
            String savedFileName = UUID.randomUUID().toString() + "_" + originalFileName;

            // Download file with proper connection handling
            try (java.io.InputStream in = connection.getInputStream()) {
                Path targetLocation = this.fileStorageLocation.resolve(savedFileName);
                long bytesCopied = Files.copy(in, targetLocation, StandardCopyOption.REPLACE_EXISTING);
                logger.info("File downloaded successfully: {} bytes", bytesCopied);
                
                // Verify file size (should be reasonable, not just a few KB)
                long fileSize = Files.size(targetLocation);
                if (fileSize < 1024) { // Less than 1KB is suspicious
                    logger.warn("Downloaded file is very small ({} bytes), may not be a valid file", fileSize);
                }
            } finally {
                connection.disconnect();
            }

            return savedFileName;
        } catch (Exception ex) {
            logger.error("Error downloading file from URL: {}", fileUrl, ex);
            throw new IOException("Could not download file from URL: " + fileUrl + " - " + ex.getMessage(), ex);
        }
    }

    /**
     * Get file size from saved filename
     */
    public Long getFileSize(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            return Files.size(filePath);
        } catch (IOException ex) {
            return 0L;
        }
    }
}
