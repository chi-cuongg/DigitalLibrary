package com.example.demo.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.example.demo.model.OAuthToken;
import com.example.demo.repository.OAuthTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleDriveService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveService.class);
    private static final String APPLICATION_NAME = "Digital Library";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);

    @Value("${google.drive.folder.id:1D9yGQ_xBZWe9ouRAiVYVbjC80XXFOjVO}")
    private String driveFolderId;

    @Value("${google.drive.credentials.path:credentials.json}")
    private String credentialsPath;

    @Value("${google.oauth2.client.id:}")
    private String clientId;

    @Value("${google.oauth2.client.secret:}")
    private String clientSecret;

    @Value("${google.oauth2.redirect.uri:http://localhost:8080/oauth2/callback}")
    private String redirectUri;

    @Autowired
    private OAuthTokenRepository oAuthTokenRepository;

    private Drive driveService;

    /**
     * Save OAuth token to database
     */
    public void saveOAuthToken(String refreshToken, String accessToken, Long expiresInSeconds) {
        try {
            OAuthToken token = oAuthTokenRepository.findFirstByOrderByUpdatedAtDesc()
                    .orElse(new OAuthToken());
            
            token.setRefreshToken(refreshToken);
            token.setAccessToken(accessToken);
            if (expiresInSeconds != null && expiresInSeconds > 0) {
                token.setExpiresAt(LocalDateTime.now().plusSeconds(expiresInSeconds));
            }
            
            oAuthTokenRepository.save(token);
            logger.info("✅ OAuth token saved to database");
        } catch (Exception e) {
            logger.error("❌ Error saving OAuth token: {}", e.getMessage(), e);
        }
    }

    /**
     * Get OAuth token from database
     */
    private OAuthToken getOAuthToken() {
        try {
            return oAuthTokenRepository.findFirstByOrderByUpdatedAtDesc().orElse(null);
        } catch (Exception e) {
            logger.warn("Error getting OAuth token from database: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Initialize Drive service with OAuth 2.0 credentials
     */
    private Drive getDriveService() throws IOException, GeneralSecurityException {
        if (driveService == null) {
            logger.info("🔍 Initializing Google Drive service with OAuth 2.0...");
            
            // Check if OAuth credentials are configured
            if (clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty()) {
                logger.error("================================================");
                logger.error("❌ OAuth 2.0 credentials NOT CONFIGURED!");
                logger.error("Please configure OAuth 2.0 in application.properties:");
                logger.error("google.oauth2.client.id=YOUR_CLIENT_ID");
                logger.error("google.oauth2.client.secret=YOUR_CLIENT_SECRET");
                logger.error("google.oauth2.redirect.uri=http://localhost:8080/oauth2/callback");
                logger.error("See file: OAUTH2_SETUP_GUIDE.md for detailed instructions");
                logger.error("================================================");
                logger.warn("⚠️ Google Drive integration is DISABLED. Files will be saved locally.");
                return null;
            }
            
            // Get refresh token from database
            OAuthToken token = getOAuthToken();
            if (token == null || token.getRefreshToken() == null || token.getRefreshToken().isEmpty()) {
                logger.warn("⚠️ No OAuth token found. Please authorize the application first.");
                logger.warn("⚠️ Visit /oauth2/authorize to start authorization");
                return null;
            }
            
            try {
                NetHttpTransport HTTP_TRANSPORT = com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport();
                
                // Build authorization code flow
                GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY,
                        clientId, clientSecret, SCOPES)
                        .setAccessType("offline")
                        .build();
                
                // Create credential from refresh token
                com.google.api.client.auth.oauth2.Credential credential = new com.google.api.client.auth.oauth2.Credential.Builder(
                        com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod())
                        .setTransport(HTTP_TRANSPORT)
                        .setJsonFactory(JSON_FACTORY)
                        .setTokenServerUrl(new com.google.api.client.http.GenericUrl("https://oauth2.googleapis.com/token"))
                        .setClientAuthentication(new com.google.api.client.http.BasicAuthentication(clientId, clientSecret))
                        .build();
                
                credential.setRefreshToken(token.getRefreshToken());
                
                // Refresh the access token if expired or not set
                if (credential.getAccessToken() == null || 
                    (token.getExpiresAt() != null && token.getExpiresAt().isBefore(LocalDateTime.now()))) {
                    credential.refreshToken();
                    logger.info("✅ Access token refreshed successfully");
                    
                    // Save updated token
                    if (credential.getExpiresInSeconds() != null) {
                        saveOAuthToken(token.getRefreshToken(), credential.getAccessToken(), credential.getExpiresInSeconds());
                    }
                } else {
                    credential.setAccessToken(token.getAccessToken());
                    logger.info("✅ Using existing access token");
                }
                
                // Build Drive service
                driveService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                        .setApplicationName(APPLICATION_NAME)
                        .build();
                
                logger.info("✅✅✅ Google Drive service initialized successfully with OAuth 2.0! ✅✅✅");
                logger.info("📁 Target folder ID: {}", driveFolderId);
            } catch (Exception e) {
                logger.error("❌❌❌ ERROR initializing Google Drive service: {}", e.getMessage(), e);
                if (e.getCause() != null) {
                    logger.error("Cause: {}", e.getCause().getMessage());
                }
                throw e;
            }
        }
        return driveService;
    }

    /**
     * Get OAuth authorization URL
     */
    public String getAuthorizationUrl() throws IOException, GeneralSecurityException {
        NetHttpTransport HTTP_TRANSPORT = com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport();
        
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY,
                clientId, clientSecret, SCOPES)
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
        
        return flow.newAuthorizationUrl().setRedirectUri(redirectUri).build();
    }

    /**
     * Exchange authorization code for tokens
     */
    public void exchangeCodeForTokens(String code) throws IOException, GeneralSecurityException {
        NetHttpTransport HTTP_TRANSPORT = com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport();
        
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY,
                clientId, clientSecret, SCOPES)
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
        
        GoogleTokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
        
        String refreshToken = response.getRefreshToken();
        String accessToken = response.getAccessToken();
        Long expiresIn = response.getExpiresInSeconds();
        
        if (refreshToken != null) {
            saveOAuthToken(refreshToken, accessToken, expiresIn);
            logger.info("✅✅✅ OAuth tokens saved successfully! ✅✅✅");
            
            // Reset driveService to force re-initialization with new token
            driveService = null;
        } else {
            logger.warn("⚠️ No refresh token received. Access token may expire soon.");
            if (accessToken != null) {
                saveOAuthToken("", accessToken, expiresIn);
            }
        }
    }
    
    /**
     * Check if Google Drive service is available
     * @return true if Drive service is ready, false otherwise
     */
    public boolean isDriveAvailable() {
        try {
            Drive drive = getDriveService();
            if (drive == null) {
                return false;
            }
            
            // Try to verify folder access
            try {
                File folder = drive.files().get(driveFolderId)
                        .setFields("id, name, mimeType")
                        .execute();
                logger.info("✅ Verified access to folder: {} ({})", folder.getName(), folder.getId());
                return true;
            } catch (Exception e) {
                logger.error("❌ Cannot access folder {}: {}", driveFolderId, e.getMessage());
                logger.error("⚠️ Make sure OAuth is authorized and folder exists");
                return false;
            }
        } catch (Exception e) {
            logger.warn("Google Drive service is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Upload file to Google Drive
     * @param fileInputStream Input stream of the file to upload
     * @param fileName Name of the file
     * @param mimeType MIME type of the file
     * @return File ID of the uploaded file
     */
    public String uploadFile(InputStream fileInputStream, String fileName, String mimeType) throws IOException, GeneralSecurityException {
        Drive drive = getDriveService();
        if (drive == null) {
            throw new IOException("Google Drive service is not available. Please authorize OAuth 2.0 first by visiting /oauth2/authorize");
        }
        
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        if (driveFolderId != null && !driveFolderId.isEmpty()) {
            fileMetadata.setParents(Collections.singletonList(driveFolderId));
            logger.info("📁 Setting parent folder: {}", driveFolderId);
        } else {
            logger.warn("⚠️ No folder ID specified! File will be uploaded to root Drive.");
        }
        
        InputStreamContent mediaContent = new InputStreamContent(mimeType != null ? mimeType : "application/pdf", fileInputStream);
        
        logger.info("📤 Starting upload to Google Drive folder: {}", driveFolderId);
        logger.info("📄 File name: {}, MIME type: {}", fileName, mimeType);
        
        try {
            File uploadedFile = drive.files().create(fileMetadata, mediaContent)
                    .setFields("id, name, webViewLink, webContentLink, size, mimeType, parents")
                    .execute();
            
            logger.info("✅✅✅ SUCCESS! File uploaded to Google Drive! ✅✅✅");
            logger.info("📄 File name: {}", uploadedFile.getName());
            logger.info("🆔 Drive File ID: {}", uploadedFile.getId());
            logger.info("📊 File size: {} bytes ({} MB)", 
                    uploadedFile.getSize() != null ? uploadedFile.getSize() : 0, 
                    uploadedFile.getSize() != null ? uploadedFile.getSize() / 1024.0 / 1024.0 : 0);
            logger.info("🔗 Web view link: {}", uploadedFile.getWebViewLink());
            logger.info("📁 Parent folder IDs: {}", uploadedFile.getParents());
            logger.info("🎯 Expected folder ID: {}", driveFolderId);
            
            // Verify file is in the correct folder
            if (uploadedFile.getParents() != null && !uploadedFile.getParents().isEmpty()) {
                String actualParentId = uploadedFile.getParents().get(0);
                if (actualParentId.equals(driveFolderId)) {
                    logger.info("✅ Verified: File is in the correct folder!");
                } else {
                    logger.warn("⚠️ WARNING: File uploaded to different folder! Expected: {}, Actual: {}", 
                            driveFolderId, actualParentId);
                }
            } else {
                logger.warn("⚠️ WARNING: File has no parent folder!");
            }
            
            return uploadedFile.getId();
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            logger.error("❌❌❌ Google Drive API Error! ❌❌❌");
            logger.error("Error code: {}", e.getStatusCode());
            logger.error("Error message: {}", e.getMessage());
            if (e.getDetails() != null) {
                logger.error("Error details: {}", e.getDetails().toString());
            }
            
            // Check for Service Account storage quota error
            if (e.getStatusCode() == 403 && e.getMessage() != null && 
                e.getMessage().contains("storageQuotaExceeded")) {
                logger.error("================================================");
                logger.error("⚠️ SERVICE ACCOUNT LIMITATION DETECTED!");
                logger.error("Service Accounts cannot upload files to shared folders.");
                logger.error("This is a Google Drive API limitation.");
                logger.error("Solutions:");
                logger.error("1. Use OAuth 2.0 instead of Service Account");
                logger.error("2. Use Google Workspace Shared Drives");
                logger.error("3. Continue using local storage (current fallback)");
                logger.error("See: SERVICE_ACCOUNT_LIMITATION.md for details");
                logger.error("================================================");
            }
            
            throw new IOException("Failed to upload file to Google Drive: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("❌❌❌ Unexpected error during upload! ❌❌❌");
            logger.error("Error: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Upload file from URL to Google Drive
     * @param fileUrl URL of the file to download and upload
     * @param fileName Name for the file
     * @return File ID of the uploaded file
     */
    public String uploadFileFromUrl(String fileUrl, String fileName) throws IOException, GeneralSecurityException {
        logger.info("Downloading file from URL to upload to Google Drive: {}", fileUrl);
        
        // Handle Google Drive links - convert to direct download format
        String downloadUrl = fileUrl;
        if (fileUrl != null && (fileUrl.contains("docs.google.com") || fileUrl.contains("drive.google.com"))) {
            logger.info("Detected Google Drive link: {}", fileUrl);
            
            if (fileUrl.contains("uc?id=")) {
                if (!fileUrl.contains("export=download")) {
                    downloadUrl = fileUrl + (fileUrl.contains("?") ? "&" : "?") + "export=download";
                }
            } else if (fileUrl.contains("/file/d/")) {
                int start = fileUrl.indexOf("/file/d/") + 8;
                int end = fileUrl.indexOf("/", start);
                if (end == -1) {
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
                int start = fileUrl.indexOf("/open?id=") + 9;
                int end = fileUrl.indexOf("&", start);
                if (end == -1) end = fileUrl.length();
                String fileId = fileUrl.substring(start, end);
                downloadUrl = "https://docs.google.com/uc?id=" + fileId + "&export=download";
                logger.info("Converted to direct download URL: {}", downloadUrl);
            }
        }
        
        // Download file to temporary location
        java.net.URI uri = java.net.URI.create(downloadUrl);
        java.net.URL url = uri.toURL();
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setRequestProperty("Accept", "*/*");
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);
        
        // Handle redirects manually
        int responseCode = connection.getResponseCode();
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
        }
        
        if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error code: " + responseCode);
        }
        
        // Check content type
        String contentType = connection.getContentType();
        logger.info("Content-Type: {}", contentType);
        
        if (contentType != null && contentType.startsWith("text/html")) {
            throw new IOException("Received HTML instead of file. Content-Type: " + contentType);
        }
        
        // Google Drive often returns application/octet-stream instead of application/pdf
        // We'll check the file content later if needed, but for now accept both
        String mimeType = "application/pdf";
        if (contentType != null && contentType.contains("application/pdf")) {
            mimeType = "application/pdf";
        } else if (contentType != null && contentType.equals("application/octet-stream")) {
            // Google Drive files are often returned as octet-stream, but they're actually PDFs
            // We'll upload with PDF mime type since the filename indicates it's a PDF
            logger.info("Content-Type is application/octet-stream (common for Google Drive), assuming PDF based on URL/filename");
            mimeType = "application/pdf";
        } else if (contentType == null) {
            // If no content type, assume PDF if filename ends with .pdf
            logger.info("No Content-Type header, assuming PDF based on URL/filename");
            mimeType = "application/pdf";
        } else {
            // For other content types, reject
            throw new IOException("File is not a PDF. Content-Type: " + contentType);
        }
        
        // Get filename from Content-Disposition header
        String finalFileName = fileName;
        if (finalFileName == null || finalFileName.isEmpty() || finalFileName.equals("uc")) {
            String contentDisposition = connection.getHeaderField("Content-Disposition");
            if (contentDisposition != null && contentDisposition.contains("filename")) {
                try {
                    int filenameIndex = contentDisposition.indexOf("filename");
                    int start = contentDisposition.indexOf("=", filenameIndex) + 1;
                    if (start > 0) {
                        String filenamePart = contentDisposition.substring(start).trim();
                        if (filenamePart.startsWith("\"") || filenamePart.startsWith("'")) {
                            char quote = filenamePart.charAt(0);
                            int end = filenamePart.indexOf(quote, 1);
                            if (end > 0) {
                                finalFileName = filenamePart.substring(1, end);
                                finalFileName = java.net.URLDecoder.decode(finalFileName, "UTF-8");
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error parsing Content-Disposition header: {}", e.getMessage());
                }
            }
            if (finalFileName == null || finalFileName.isEmpty() || finalFileName.equals("uc")) {
                finalFileName = "book.pdf";
            }
        }
        
        // Download to temporary file
        Path tempFile = Files.createTempFile("drive_upload_", ".pdf");
        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            connection.disconnect();
        }
        
        // Upload to Drive
        long tempFileSize = Files.size(tempFile);
        logger.info("📦 Temporary file created: {} bytes ({} MB)", tempFileSize, tempFileSize / 1024.0 / 1024.0);
        
        try (FileInputStream fileInputStream = new FileInputStream(tempFile.toFile())) {
            logger.info("🚀 Starting upload to Google Drive...");
            String fileId = uploadFile(fileInputStream, finalFileName, mimeType);
            logger.info("✅✅✅ Upload completed! File ID: {} ✅✅✅", fileId);
            return fileId;
        } catch (Exception e) {
            logger.error("❌❌❌ Upload to Google Drive FAILED! ❌❌❌");
            logger.error("Error: {}", e.getMessage(), e);
            throw e;
        } finally {
            // Clean up temporary file
            try {
                Files.deleteIfExists(tempFile);
                logger.info("🗑️ Temporary file cleaned up");
            } catch (Exception e) {
                logger.warn("⚠️ Failed to delete temporary file: {}", e.getMessage());
            }
        }
    }

    /**
     * Get download URL for a file by its ID
     * @param fileId Google Drive file ID
     * @return Direct download URL
     */
    public String getDownloadUrl(String fileId) throws IOException, GeneralSecurityException {
        Drive drive = getDriveService();
        if (drive == null) {
            throw new IOException("Google Drive service is not available. Please authorize OAuth 2.0 first by visiting /oauth2/authorize");
        }
        File file = drive.files().get(fileId).setFields("webContentLink, id, name").execute();
        // Return direct download URL (modify webContentLink to force download)
        String webContentLink = file.getWebContentLink();
        if (webContentLink != null && webContentLink.contains("&export=download") == false) {
            // Add export=download parameter
            webContentLink = webContentLink + (webContentLink.contains("?") ? "&" : "?") + "export=download";
        }
        return webContentLink != null ? webContentLink : "https://drive.google.com/uc?id=" + fileId + "&export=download";
    }

    /**
     * Get file metadata
     * @param fileId Google Drive file ID
     * @return File metadata
     */
    public File getFileMetadata(String fileId) throws IOException, GeneralSecurityException {
        Drive drive = getDriveService();
        if (drive == null) {
            throw new IOException("Google Drive service is not available. Please authorize OAuth 2.0 first by visiting /oauth2/authorize");
        }
        return drive.files().get(fileId)
                .setFields("id, name, size, mimeType, webViewLink, webContentLink")
                .execute();
    }

    /**
     * Download file from Google Drive
     * @param fileId Google Drive file ID
     * @return InputStream of the file
     */
    public InputStream downloadFile(String fileId) throws IOException, GeneralSecurityException {
        Drive drive = getDriveService();
        if (drive == null) {
            throw new IOException("Google Drive service is not available. Please authorize OAuth 2.0 first by visiting /oauth2/authorize");
        }
        HttpResponse response = drive.files().get(fileId).executeMedia();
        return response.getContent();
    }

    /**
     * Delete file from Google Drive
     * @param fileId Google Drive file ID
     */
    public void deleteFile(String fileId) throws IOException, GeneralSecurityException {
        Drive drive = getDriveService();
        if (drive == null) {
            throw new IOException("Google Drive service is not available. Please authorize OAuth 2.0 first by visiting /oauth2/authorize");
        }
        drive.files().delete(fileId).execute();
        logger.info("File deleted from Google Drive: {}", fileId);
    }
}
