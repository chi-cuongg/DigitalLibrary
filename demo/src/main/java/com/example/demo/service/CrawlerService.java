package com.example.demo.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Book;
import com.example.demo.model.BookFile;
import com.example.demo.model.Category;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.CategoryRepository;
import com.google.api.services.drive.model.File;

@Service
public class CrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);
    private static final String BASE_URL = "https://dtv-ebook.com.vn";
    private static final int TIMEOUT = 30000; // 30 seconds

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private GoogleDriveService googleDriveService;

    /**
     * Crawl books from a specific URL
     */
    @Transactional
    public CrawlResult crawlBooksFromUrl(String url, int maxBooks, boolean downloadFiles) {
        CrawlResult result = new CrawlResult();
        result.setStartTime(System.currentTimeMillis());

        try {
            logger.info("Starting crawl from URL: {}", url);

            // Check if URL is a book detail page
            if (isBookDetailPage(url)) {
                // Crawl single book detail page
                logger.info("Detected book detail page, crawling single book");
                try {
                    BookInfo bookInfo = crawlBookDetailPage(url);
                    if (bookInfo != null && bookInfo.getTitle() != null && !bookInfo.getTitle().isEmpty()) {
                        logger.info("Successfully extracted book info, attempting to save...");
                        try {
                            Book savedBook = saveBook(bookInfo, downloadFiles);
                            if (savedBook != null) {
                                result.incrementSuccess();
                                logger.info("SUCCESS: Saved book: {}", savedBook.getTitle());
                            } else {
                                result.incrementSkipped();
                                logger.warn("Skipped book (possibly duplicate): {}", bookInfo.getTitle());
                            }
                        } catch (Exception e) {
                            result.incrementFailed();
                            logger.error("FAILED to save book '{}': {}", bookInfo.getTitle(), e.getMessage(), e);
                            result.addError("Lỗi khi lưu sách '" + bookInfo.getTitle() + "': " + e.getMessage());
                        }
                    } else {
                        result.incrementFailed();
                        if (bookInfo == null) {
                            logger.error("FAILED: crawlBookDetailPage returned null - likely an exception occurred");
                            result.addError("Không thể crawl trang chi tiết. Kiểm tra log để xem chi tiết lỗi.");
                        } else {
                            logger.error("FAILED: No title extracted. Title was: '{}'", bookInfo.getTitle());
                            result.addError("Không thể trích xuất tiêu đề sách từ trang web.");
                        }
                    }
                } catch (Exception e) {
                    result.incrementFailed();
                    logger.error("FAILED to crawl book detail page: {}", e.getMessage(), e);
                    result.addError("Lỗi khi crawl trang chi tiết: " + e.getMessage());
                }
            } else {
                // Crawl list page - find all book detail page links
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(TIMEOUT)
                        .followRedirects(true)
                        .get();

                logger.info("Crawling list page, looking for book detail links");

                // Find all links that look like book detail pages
                Elements allLinks = doc.select("a[href]");
                Set<String> bookDetailUrls = new HashSet<>();

                for (Element link : allLinks) {
                    String href = link.attr("href");
                    if (href != null && !href.isEmpty()) {
                        // Normalize URL
                        if (href.startsWith("//")) {
                            href = "https:" + href;
                        } else if (href.startsWith("/")) {
                            href = BASE_URL + href;
                        } else if (!href.startsWith("http")) {
                            if (href.contains("dtv-ebook.com.vn")) {
                                href = "https://" + href;
                            } else {
                                href = BASE_URL + "/" + href;
                            }
                        }

                        // Check if it's a book detail page URL
                        if (isBookDetailPage(href)) {
                            // Remove fragment
                            String cleanHref = href.split("#")[0];
                            if (cleanHref.contains("dtv-ebook.com.vn") && cleanHref.endsWith(".html")) {
                                bookDetailUrls.add(cleanHref);
                            }
                        }
                    }
                }

                logger.info("Found {} unique book detail page URLs", bookDetailUrls.size());

                int processed = 0;
                for (String detailUrl : bookDetailUrls) {
                    if (processed >= maxBooks) {
                        break;
                    }

                    try {
                        logger.info("Crawling book detail page {}/{}: {}", processed + 1,
                                Math.min(maxBooks, bookDetailUrls.size()), detailUrl);
                        BookInfo bookInfo = crawlBookDetailPage(detailUrl);
                        if (bookInfo != null && bookInfo.getTitle() != null && !bookInfo.getTitle().isEmpty()) {
                            Book savedBook = saveBook(bookInfo, downloadFiles);
                            if (savedBook != null) {
                                result.incrementSuccess();
                                logger.info("Saved book: {}", savedBook.getTitle());
                            } else {
                                result.incrementSkipped();
                                logger.warn("Skipped book (possibly duplicate): {}", bookInfo.getTitle());
                            }
                        } else {
                            result.incrementFailed();
                            logger.warn("Failed to extract book info from detail page: {}", detailUrl);
                        }
                        processed++;

                        // Add delay to avoid overwhelming the server
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        result.incrementFailed();
                        logger.error("Error processing book detail page {}: {}", detailUrl, e.getMessage(), e);
                    }
                }

                if (bookDetailUrls.isEmpty()) {
                    logger.warn(
                            "No book detail page URLs found. The page might not contain book links or has a different structure.");
                    result.addError(
                            "Không tìm thấy link trang chi tiết sách. Vui lòng nhập URL trang chi tiết sách trực tiếp.");
                }
            }

        } catch (IOException e) {
            logger.error("Error connecting to URL: {}", url, e);
            result.addError("Connection error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during crawl: {}", e.getMessage(), e);
            result.addError("Unexpected error: " + e.getMessage());
        }

        result.setEndTime(System.currentTimeMillis());
        result.setDuration(result.getEndTime() - result.getStartTime());
        logger.info("Crawl completed. Success: {}, Failed: {}, Skipped: {}",
                result.getSuccessCount(), result.getFailedCount(), result.getSkippedCount());

        return result;
    }

    /**
     * Check if URL is a book detail page (dtv-ebook.com.vn pattern)
     */
    private boolean isBookDetailPage(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        // Pattern: https://dtv-ebook.com.vn/book-name_ID.html
        // Remove fragment if present
        String cleanUrl = url.split("#")[0];
        return cleanUrl.contains("dtv-ebook.com.vn") &&
                (cleanUrl.contains(".html") || cleanUrl.matches(".*dtv-ebook\\.com\\.vn/[^/]+_[0-9]+\\.html.*"));
    }

    /**
     * Crawl book information from detail page
     */
    private BookInfo crawlBookDetailPage(String url) {
        BookInfo info = new BookInfo();
        info.setDetailUrl(url);

        try {
            // Remove fragment from URL if present
            String cleanUrl = url.split("#")[0];

            logger.info("Crawling book detail page: {}", cleanUrl);
            Document doc = Jsoup.connect(cleanUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .get();

            logger.info("Successfully fetched page, extracting information...");

            // Extract title - dtv-ebook.com.vn uses h2.ten_san_pham
            try {
                Element titleElement = doc.selectFirst("h2.ten_san_pham");
                if (titleElement == null) {
                    titleElement = doc.selectFirst("h2.text-center.ten_san_pham");
                }
                if (titleElement == null) {
                    titleElement = doc.selectFirst(".ten_san_pham");
                }

                if (titleElement != null) {
                    String title = titleElement.text().trim();
                    logger.info("Found title element, text: '{}'", title);
                    if (!title.isEmpty()) {
                        info.setTitle(title);
                        logger.info("Title extracted: {}", title);
                    }
                } else {
                    logger.warn("Could not find h2.ten_san_pham, trying fallback selectors");
                }
            } catch (Exception e) {
                logger.error("Error extracting title: {}", e.getMessage());
            }

            // Fallback: try other common selectors
            if (info.getTitle() == null || info.getTitle().isEmpty()) {
                try {
                    Element titleElement = doc.selectFirst("h2");
                    if (titleElement == null) {
                        titleElement = doc.selectFirst("h1");
                    }
                    if (titleElement != null) {
                        String title = titleElement.text().trim();
                        logger.info("Found fallback title: '{}'", title);
                        if (!title.isEmpty() && !title.contains("Tải Ebook") && !title.contains("DTV eBook")) {
                            info.setTitle(title);
                            logger.info("Title extracted from fallback: {}", title);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error extracting fallback title: {}", e.getMessage());
                }
            }

            // Last resort: page title
            if (info.getTitle() == null || info.getTitle().isEmpty()) {
                try {
                    String pageTitle = doc.title();
                    logger.info("Page title: '{}'", pageTitle);
                    if (pageTitle != null && !pageTitle.isEmpty()) {
                        pageTitle = pageTitle.replace(" | dtv-ebook.com.vn", "").replace(" - dtv-ebook.com.vn", "")
                                .trim();
                        if (!pageTitle.isEmpty() && !pageTitle.equals("dtv-ebook.com.vn")
                                && !pageTitle.contains("Tải Ebook")) {
                            info.setTitle(pageTitle);
                            logger.info("Title extracted from page title: {}", pageTitle);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error extracting page title: {}", e.getMessage());
                }
            }

            if (info.getTitle() == null || info.getTitle().isEmpty()) {
                logger.error("CRITICAL: Could not extract title from page!");
            }

            // Extract author - dtv-ebook.com.vn has author in table row
            try {
                Elements tableRows = doc.select("table.tblChiTietDiDong tr");
                logger.info("Found {} table rows to search for author", tableRows.size());

                for (Element row : tableRows) {
                    Elements tds = row.select("td");
                    if (tds.size() >= 2) {
                        String firstCell = tds.get(0).text().trim();
                        logger.debug("Checking table row, first cell: '{}'", firstCell);

                        if (firstCell.contains("Tác giả") || firstCell.contains("Tác Giả")
                                || firstCell.equalsIgnoreCase("Tác giả")) {
                            logger.info("Found 'Tác giả' row");
                            Element authorLink = tds.get(1).selectFirst("a");
                            if (authorLink != null) {
                                String author = authorLink.text().trim();
                                logger.info("Author from link: '{}'", author);
                                if (!author.isEmpty()) {
                                    info.setAuthor(author);
                                    break;
                                }
                            } else {
                                // If no link, get text from second td
                                String author = tds.get(1).text().trim();
                                logger.info("Author from text: '{}'", author);
                                if (!author.isEmpty()) {
                                    info.setAuthor(author);
                                    break;
                                }
                            }
                        }
                    }
                }

                if (info.getAuthor() == null || info.getAuthor().isEmpty()) {
                    logger.warn("Could not find author in table, trying meta tags");
                }
            } catch (Exception e) {
                logger.error("Error extracting author: {}", e.getMessage(), e);
            }

            // Fallback: try meta tags
            if (info.getAuthor() == null || info.getAuthor().isEmpty()) {
                try {
                    Elements metaAuthors = doc.select("meta[property='book:author'], meta[name='author']");
                    logger.info("Found {} meta author tags", metaAuthors.size());
                    for (Element meta : metaAuthors) {
                        String author = meta.attr("content");
                        if (author != null && !author.isEmpty()) {
                            info.setAuthor(author.trim());
                            logger.info("Author from meta: {}", author);
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error extracting author from meta: {}", e.getMessage());
                }
            }

            // Extract description - dtv-ebook.com.vn uses div#chitiet with tab content
            try {
                Element descElement = doc.selectFirst("#chitiet.content");
                if (descElement == null) {
                    descElement = doc.selectFirst("#chitiet");
                }
                if (descElement == null) {
                    descElement = doc.selectFirst(".content#chitiet");
                }

                if (descElement != null) {
                    logger.info("Found description element");
                    // Remove unwanted elements
                    descElement.select(
                            "script, style, .ads, .advertisement, .download-box, .download-link, .faq, iframe, .fb-like")
                            .remove();

                    // Get all paragraphs but skip FAQ and download sections
                    Elements paragraphs = descElement.select("p");
                    logger.info("Found {} paragraphs in description", paragraphs.size());

                    StringBuilder description = new StringBuilder();
                    for (Element p : paragraphs) {
                        String text = p.text().trim();
                        if (text != null && !text.isEmpty() && text.length() > 30
                                && !text.toLowerCase().startsWith("xem thêm")
                                && !text.toLowerCase().startsWith("tải về")
                                && !text.toLowerCase().startsWith("download")
                                && !text.toLowerCase().contains("click here")
                                && !text.toLowerCase().contains("đăng ký")
                                && !text.toLowerCase().contains("đăng nhập")
                                && !text.toLowerCase().contains("faq")
                                && !text.toLowerCase().contains("câu hỏi thường")
                                && !text.toUpperCase().equals("THÔNG TIN TÁC GIẢ")) {
                            if (description.length() > 0) {
                                description.append("\n\n");
                            }
                            description.append(text);
                        }
                    }

                    // If we got paragraphs, use them
                    if (description.length() > 0) {
                        info.setDescription(description.toString());
                        logger.info("Description extracted, length: {}", description.length());
                    } else {
                        // Fallback: get all text but clean it
                        String fullText = descElement.text().trim();
                        // Remove common unwanted patterns
                        fullText = fullText.replaceAll("(?i)(xem thêm|tải về|download|click here|faq|câu hỏi).*", "");
                        if (fullText.length() > 50) {
                            info.setDescription(fullText);
                            logger.info("Description extracted from full text, length: {}", fullText.length());
                        }
                    }
                } else {
                    logger.warn("Could not find #chitiet element");
                }
            } catch (Exception e) {
                logger.error("Error extracting description: {}", e.getMessage(), e);
            }

            // If still no description, try other selectors
            if (info.getDescription() == null || info.getDescription().isEmpty()) {
                try {
                    Element descElement = doc.selectFirst(".chitiet, .entry-content, article");
                    if (descElement != null) {
                        descElement.select("script, style, .download-box, .faq").remove();
                        String text = descElement.text().trim();
                        if (text.length() > 50) {
                            info.setDescription(text);
                            logger.info("Description extracted from fallback selector");
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error extracting description from fallback: {}", e.getMessage());
                }
            }

            // Extract image URL - dtv-ebook.com.vn uses img.hinhdaidien
            Element imgElement = doc.selectFirst("img.hinhdaidien, .hinhdaidien");
            if (imgElement != null) {
                String imgSrc = imgElement.attr("src");
                if (imgSrc == null || imgSrc.isEmpty()) {
                    imgSrc = imgElement.attr("data-src"); // Lazy loading
                }
                if (imgSrc != null && !imgSrc.isEmpty()) {
                    if (imgSrc.startsWith("//")) {
                        imgSrc = "https:" + imgSrc;
                    } else if (imgSrc.startsWith("/")) {
                        imgSrc = BASE_URL + imgSrc;
                    } else if (!imgSrc.startsWith("http")) {
                        imgSrc = BASE_URL + "/" + imgSrc;
                    }
                    info.setImageUrl(imgSrc);
                }
            }

            // Fallback: try other image selectors
            if (info.getImageUrl() == null || info.getImageUrl().isEmpty()) {
                imgElement = doc.selectFirst("img[src*='images/files'], .book-cover img, img");
                if (imgElement != null) {
                    String imgSrc = imgElement.attr("src");
                    if (imgSrc != null && !imgSrc.isEmpty() && !imgSrc.contains("banner") && !imgSrc.contains("logo")) {
                        if (imgSrc.startsWith("//")) {
                            imgSrc = "https:" + imgSrc;
                        } else if (imgSrc.startsWith("/")) {
                            imgSrc = BASE_URL + imgSrc;
                        } else if (!imgSrc.startsWith("http")) {
                            imgSrc = BASE_URL + "/" + imgSrc;
                        }
                        info.setImageUrl(imgSrc);
                    }
                }
            }

            // Extract download link - dtv-ebook.com.vn has download links in #download tab
            try {
                // Look for links in download section first
                Element downloadTab = doc.selectFirst("#download.content");
                if (downloadTab == null) {
                    downloadTab = doc.selectFirst(".content#download");
                }
                if (downloadTab == null) {
                    downloadTab = doc.selectFirst("#download");
                }

                if (downloadTab != null) {
                    logger.info("Found download tab element");
                    Elements downloadLinks = downloadTab.select("a[href]");
                    logger.info("Found {} links in download tab", downloadLinks.size());

                    // First pass: look for PDF links only
                    boolean foundPdf = false;
                    for (Element link : downloadLinks) {
                        // Use abs:href to get absolute URL
                        String href = link.attr("abs:href");
                        if (href == null || href.isEmpty()) {
                            href = link.attr("href");
                        }
                        String text = link.text().toLowerCase();
                        logger.info("Checking download link: href='{}', text='{}'", href, text);

                        // Skip if it's clearly NOT PDF (EPUB, MOBI, AZW3, etc.)
                        if (href.contains(".epub") || href.contains(".mobi") || href.contains(".azw3") ||
                                href.contains(".azw") || href.contains(".mp3") || href.contains(".doc") ||
                                href.contains(".docx") || text.contains("epub") || text.contains("mobi") ||
                                text.contains("azw3") || text.contains("azw") || text.contains("mp3")) {
                            logger.info("Skipping non-PDF link (detected other format): {}", href);
                            continue;
                        }

                        // Check for PDF links only (must explicitly contain PDF)
                        if (href != null && !href.isEmpty()) {
                            // Must contain .pdf in URL or "pdf" in text, or be a Google Drive link that
                            // we'll verify later
                            boolean isPdfLink = href.contains(".pdf") || text.contains("pdf") ||
                                    (href.contains("docs.google.com") || href.contains("drive.google.com"));

                            if (isPdfLink) {
                                // Double check: skip if text explicitly mentions other formats
                                if (text.contains("epub") || text.contains("mobi") || text.contains("azw") ||
                                        text.contains("mp3") || text.contains("audio")) {
                                    logger.info("Skipping link - text mentions non-PDF format: {}", href);
                                    continue;
                                }

                                // Normalize URL - ensure it's absolute
                                if (href.startsWith("//")) {
                                    href = "https:" + href;
                                } else if (!href.startsWith("http")) {
                                    // Try to make it absolute using base URL
                                    if (href.startsWith("/")) {
                                        href = BASE_URL + href;
                                    } else {
                                        href = BASE_URL + "/" + href;
                                    }
                                }

                                // Validate URL before setting
                                if (href.length() > 10 && (href.startsWith("http://") || href.startsWith("https://"))) {
                                    info.setDownloadUrl(href);
                                    logger.info("Found PDF download URL from download tab: {}", href);
                                    foundPdf = true;
                                    break;
                                } else {
                                    logger.warn("Invalid URL format: {}", href);
                                }
                            }
                        }
                    }

                    // If no PDF found in first pass, skip EPUB/MOBI
                    if (!foundPdf) {
                        logger.info("No PDF link found in download tab, will try other sections");
                    }
                } else {
                    logger.warn("Could not find #download tab");
                }

                // If not found, look in download-box (PDF only)
                if (info.getDownloadUrl() == null || info.getDownloadUrl().isEmpty()) {
                    Element downloadBox = doc.selectFirst(".download-box");
                    if (downloadBox != null) {
                        logger.info("Found download-box element");
                        Elements downloadBoxLinks = downloadBox.select("a[href]");
                        for (Element downloadLink : downloadBoxLinks) {
                            String href = downloadLink.attr("abs:href");
                            if (href == null || href.isEmpty()) {
                                href = downloadLink.attr("href");
                            }
                            String text = downloadLink.text().toLowerCase();

                            // Skip non-PDF formats
                            if (href.contains(".epub") || href.contains(".mobi") || href.contains(".azw3") ||
                                    href.contains(".azw") || text.contains("epub") || text.contains("mobi") ||
                                    text.contains("azw3") || text.contains("azw")) {
                                logger.info("Skipping non-PDF link from download-box: {}", href);
                                continue;
                            }

                            // Only accept PDF links
                            if (href != null && !href.isEmpty() &&
                                    (href.contains(".pdf") || text.contains("pdf") ||
                                            href.contains("docs.google.com") || href.contains("drive.google.com"))) {
                                if (href.startsWith("//")) {
                                    href = "https:" + href;
                                } else if (!href.startsWith("http")) {
                                    href = "https://" + href;
                                }
                                info.setDownloadUrl(href);
                                logger.info("Found PDF download URL from download-box: {}", href);
                                break;
                            }
                        }
                    }
                }

                // Fallback: search all links (PDF only)
                if (info.getDownloadUrl() == null || info.getDownloadUrl().isEmpty()) {
                    logger.info("Searching all links for PDF download URL...");
                    // Only search for PDF links - explicitly exclude other formats
                    Elements allDownloadLinks = doc.select(
                            "a[href*='docs.google.com'], a[href*='drive.google.com'], a[href$='.pdf'], a[href*='.pdf']");
                    logger.info("Found {} potential download links", allDownloadLinks.size());

                    for (Element link : allDownloadLinks) {
                        // Use abs:href to get absolute URL
                        String href = link.attr("abs:href");
                        if (href == null || href.isEmpty()) {
                            href = link.attr("href");
                        }
                        String text = link.text().toLowerCase();
                        logger.info("Checking potential download link: href='{}', text='{}'", href, text);

                        // Skip all non-PDF formats explicitly
                        if (href.contains(".epub") || href.contains(".mobi") || href.contains(".azw3") ||
                                href.contains(".azw") || href.contains(".mp3") || href.contains(".doc") ||
                                href.contains(".docx") || text.contains("epub") || text.contains("mobi") ||
                                text.contains("azw3") || text.contains("azw") || text.contains("mp3") ||
                                text.contains("audio")) {
                            logger.info("Skipping non-PDF link: {}", href);
                            continue;
                        }

                        // Only accept if it explicitly contains .pdf or text says "pdf"
                        if (href != null && !href.isEmpty() &&
                                (href.contains(".pdf") || text.contains("pdf") ||
                                        (href.contains("docs.google.com") || href.contains("drive.google.com")))) {

                            // Normalize URL - ensure it's absolute
                            if (href.startsWith("//")) {
                                href = "https:" + href;
                            } else if (!href.startsWith("http")) {
                                if (href.startsWith("/")) {
                                    href = BASE_URL + href;
                                } else {
                                    href = BASE_URL + "/" + href;
                                }
                            }

                            // Validate URL before setting
                            if (href.length() > 10 && (href.startsWith("http://") || href.startsWith("https://"))) {
                                info.setDownloadUrl(href);
                                logger.info("Found PDF download URL from general search: {}", href);
                                break;
                            } else {
                                logger.warn("Invalid URL format: {}", href);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error extracting download URL: {}", e.getMessage(), e);
            }

            // Extract categories - dtv-ebook.com.vn has categories in table row "Thể loại"
            Elements tableRows = doc.select("table.tblChiTietDiDong tr, .tblChiTietDiDong tr");
            for (Element row : tableRows) {
                Elements tds = row.select("td");
                if (tds.size() >= 2) {
                    String firstCell = tds.get(0).text().trim();
                    if (firstCell.contains("Thể loại") || firstCell.contains("Thể Loại")) {
                        Elements categoryLinks = tds.get(1).select("a.label");
                        for (Element catLink : categoryLinks) {
                            String categoryName = catLink.text().trim();
                            if (!categoryName.isEmpty() && categoryName.length() < 100) {
                                info.addCategory(categoryName);
                            }
                        }
                        break;
                    }
                }
            }

            // Also extract from tags/keywords section
            for (Element row : tableRows) {
                Elements tds = row.select("td");
                if (tds.size() >= 2) {
                    String firstCell = tds.get(0).text().trim();
                    if (firstCell.contains("Từ khóa") || firstCell.contains("Tags")) {
                        Elements tagLinks = tds.get(1).select("a.label");
                        for (Element tagLink : tagLinks) {
                            String tag = tagLink.text().trim();
                            // Skip common tags that are not categories
                            if (!tag.isEmpty() && tag.length() < 100 &&
                                    !tag.equalsIgnoreCase("ebook") && !tag.equalsIgnoreCase("pdf") &&
                                    !tag.equalsIgnoreCase("full")) {
                                // Add if it looks like a category (capitalized words)
                                if (tag.length() > 2) {
                                    info.addCategory(tag);
                                }
                            }
                        }
                        break;
                    }
                }
            }

            logger.info(
                    "Extracted book info - Title: '{}', Author: '{}', Description length: {}, Image: {}, Download URL: {}, Categories: {}",
                    info.getTitle(),
                    info.getAuthor(),
                    info.getDescription() != null ? info.getDescription().length() : 0,
                    info.getImageUrl() != null ? "Yes" : "No",
                    info.getDownloadUrl() != null ? "Yes" : "No",
                    info.getCategories().size());

            // Final validation - if no title, this is a failure
            if (info.getTitle() == null || info.getTitle().trim().isEmpty()) {
                logger.error("FAILED: No title extracted from page {}", cleanUrl);
                return null; // Return null to indicate failure
            }

        } catch (IOException e) {
            logger.error("IO Error crawling book detail page {}: {}", url, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error crawling book detail page {}: {}", url, e.getMessage(), e);
            return null; // Return null on error so caller knows it failed
        }

        return info;
    }

    /**
     * Find book elements using various possible selectors
     */
    private Elements findBookElements(Document doc) {
        // Try different common selectors for book listings
        Elements elements = doc.select("article.book, .book-item, .book, [class*='book'], .post, .entry");

        if (elements.isEmpty()) {
            // Try finding links that might be book pages
            elements = doc.select("a[href*='/book'], a[href*='/sach'], a[href*='ebook']");
        }

        if (elements.isEmpty()) {
            // Fallback: look for any card/box structure that might contain book info
            elements = doc.select(".item, .card, .box, .product");
        }

        return elements;
    }

    /**
     * Extract book information from HTML element
     */
    private BookInfo extractBookInfo(Element element, String baseUrl) {
        BookInfo info = new BookInfo();

        try {
            // Extract title
            Element titleElement = element.selectFirst("h1, h2, h3, h4, .title, [class*='title'], a");
            if (titleElement != null) {
                info.setTitle(titleElement.text().trim());
            }

            // Extract author
            Element authorElement = element.selectFirst(".author, [class*='author'], [itemprop='author']");
            if (authorElement != null) {
                info.setAuthor(authorElement.text().trim());
            }

            // Extract description
            Element descElement = element.selectFirst(".description, .desc, .content, [class*='description'], p");
            if (descElement != null) {
                info.setDescription(descElement.text().trim());
            }

            // Extract image URL
            Element imgElement = element.selectFirst("img");
            if (imgElement != null) {
                String imgSrc = imgElement.attr("src");
                if (imgSrc != null && !imgSrc.isEmpty()) {
                    if (imgSrc.startsWith("//")) {
                        imgSrc = "https:" + imgSrc;
                    } else if (imgSrc.startsWith("/")) {
                        imgSrc = BASE_URL + imgSrc;
                    } else if (!imgSrc.startsWith("http")) {
                        imgSrc = BASE_URL + "/" + imgSrc;
                    }
                    info.setImageUrl(imgSrc);
                }
            }

            // Extract book detail URL
            Element linkElement = element.selectFirst("a");
            if (linkElement != null) {
                String href = linkElement.attr("href");
                if (href != null && !href.isEmpty()) {
                    if (href.startsWith("//")) {
                        href = "https:" + href;
                    } else if (href.startsWith("/")) {
                        href = BASE_URL + href;
                    } else if (!href.startsWith("http")) {
                        href = BASE_URL + "/" + href;
                    }
                    info.setDetailUrl(href);
                }
            }

            // Try to get more details from detail page
            if (info.getDetailUrl() != null && !info.getDetailUrl().isEmpty()) {
                enrichBookInfoFromDetailPage(info);
            }

            // Extract categories/tags
            Elements categoryElements = element
                    .select(".category, .tag, [class*='category'], [class*='tag'], a[href*='category']");
            for (Element catElement : categoryElements) {
                String categoryName = catElement.text().trim();
                if (categoryName != null && !categoryName.isEmpty() && categoryName.length() < 100) {
                    info.addCategory(categoryName);
                }
            }

            // Extract download link
            Element downloadElement = element
                    .selectFirst("a[href*='download'], a[href*='.pdf'], a[href*='.epub'], a[href*='.mobi']");
            if (downloadElement != null) {
                String downloadHref = downloadElement.attr("href");
                if (downloadHref != null && !downloadHref.isEmpty()) {
                    if (downloadHref.startsWith("//")) {
                        downloadHref = "https:" + downloadHref;
                    } else if (downloadHref.startsWith("/")) {
                        downloadHref = BASE_URL + downloadHref;
                    } else if (!downloadHref.startsWith("http")) {
                        downloadHref = BASE_URL + "/" + downloadHref;
                    }
                    info.setDownloadUrl(downloadHref);
                }
            }

        } catch (Exception e) {
            logger.warn("Error extracting book info: {}", e.getMessage());
        }

        return info;
    }

    /**
     * Enrich book info by fetching detail page
     */
    private void enrichBookInfoFromDetailPage(BookInfo info) {
        try {
            Document detailDoc = Jsoup.connect(info.getDetailUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT)
                    .followRedirects(true)
                    .get();

            // Extract more detailed description
            if (info.getDescription() == null || info.getDescription().isEmpty()) {
                Element contentElement = detailDoc.selectFirst(".content, .description, .post-content, article");
                if (contentElement != null) {
                    info.setDescription(contentElement.text().trim());
                }
            }

            // Extract author if not found
            if (info.getAuthor() == null || info.getAuthor().isEmpty()) {
                Element authorElement = detailDoc
                        .selectFirst(".author, [itemprop='author'], meta[property='book:author']");
                if (authorElement != null) {
                    String author = authorElement.hasAttr("content") ? authorElement.attr("content")
                            : authorElement.text().trim();
                    info.setAuthor(author);
                }
            }

            // Find download links
            if (info.getDownloadUrl() == null || info.getDownloadUrl().isEmpty()) {
                Elements downloadLinks = detailDoc.select(
                        "a[href*='download'], a[href*='.pdf'], a[href*='.epub'], a[href*='.mobi'], a[href*='file']");
                for (Element link : downloadLinks) {
                    String href = link.attr("href");
                    if (href != null && (href.contains(".pdf") || href.contains(".epub") ||
                            href.contains(".mobi") || href.contains("download"))) {
                        if (href.startsWith("//")) {
                            href = "https:" + href;
                        } else if (href.startsWith("/")) {
                            href = BASE_URL + href;
                        } else if (!href.startsWith("http")) {
                            href = BASE_URL + "/" + href;
                        }
                        info.setDownloadUrl(href);
                        break;
                    }
                }
            }

            // Extract categories from detail page
            Elements catElements = detailDoc
                    .select(".category, .tag, [class*='category'], [class*='tag'], a[href*='category']");
            for (Element catElement : catElements) {
                String categoryName = catElement.text().trim();
                if (categoryName != null && !categoryName.isEmpty() && categoryName.length() < 100) {
                    info.addCategory(categoryName);
                }
            }

            Thread.sleep(500); // Small delay between requests
        } catch (Exception e) {
            logger.warn("Could not enrich book info from detail page: {}", e.getMessage());
        }
    }

    /**
     * Save book to database
     */
    @Transactional
    private Book saveBook(BookInfo info, boolean downloadFile) {
        try {
            // Validate required fields
            if (info == null) {
                logger.error("Cannot save book: BookInfo is null");
                return null;
            }

            if (info.getTitle() == null || info.getTitle().trim().isEmpty()) {
                logger.error("Cannot save book: title is empty");
                return null;
            }

            logger.info("Attempting to save book: '{}'", info.getTitle());

            // Check if book already exists - use exact match for better accuracy
            List<Book> existingBooks = bookRepository.findAll();
            logger.info("Checking against {} existing books", existingBooks.size());

            for (Book existing : existingBooks) {
                // Check by title (exact or very similar)
                if (existing.getTitle() != null &&
                        existing.getTitle().trim().equalsIgnoreCase(info.getTitle().trim())) {
                    logger.info("Book already exists (duplicate title): '{}'", info.getTitle());
                    return null; // Skip duplicate
                }
            }

            logger.info("Book does not exist, creating new book...");

            Book book = new Book();
            book.setTitle(info.getTitle().trim());
            if (info.getAuthor() != null && !info.getAuthor().trim().isEmpty()) {
                book.setAuthor(info.getAuthor().trim());
            }
            if (info.getDescription() != null && !info.getDescription().trim().isEmpty()) {
                book.setDescription(info.getDescription().trim());
            }
            if (info.getImageUrl() != null && !info.getImageUrl().trim().isEmpty()) {
                book.setImageUrl(info.getImageUrl().trim());
            }

            // Handle categories - ensure at least one category
            Set<Category> categories = new HashSet<>();
            if (info.getCategories() != null && !info.getCategories().isEmpty()) {
                for (String categoryName : info.getCategories()) {
                    if (categoryName != null && !categoryName.trim().isEmpty() && categoryName.length() < 100) {
                        Category category = categoryRepository.findByName(categoryName.trim())
                                .orElseGet(() -> {
                                    Category newCategory = new Category();
                                    newCategory.setName(categoryName.trim());
                                    return categoryRepository.save(newCategory);
                                });
                        categories.add(category);
                    }
                }
            }

            // If no categories found, create or use a default category
            if (categories.isEmpty()) {
                Category defaultCategory = categoryRepository.findByName("Chưa phân loại")
                        .orElseGet(() -> {
                            Category newCategory = new Category();
                            newCategory.setName("Chưa phân loại");
                            return categoryRepository.save(newCategory);
                        });
                categories.add(defaultCategory);
            }
            book.setCategories(categories);

            // Process file: ALWAYS download to local, ALWAYS upload to Drive, delete local if checkbox not checked
            // downloadFile checkbox: true = keep file in uploads, false = delete file from uploads after upload
            if (info.getDownloadUrl() != null && !info.getDownloadUrl().isEmpty()) {
                try {
                    String fileName = extractFileNameFromUrl(info.getDownloadUrl());
                    logger.info("📥 Extracted filename: {}", fileName);

                    // Check if Google Drive is available
                    boolean driveAvailable = googleDriveService.isDriveAvailable();
                    logger.info("🔍 Google Drive availability check: {}",
                            driveAvailable ? "✅ AVAILABLE" : "❌ NOT AVAILABLE");
                    logger.info("📋 Download checkbox value: {} (true = keep in uploads, false = delete after upload)",
                            downloadFile);

                    String filePath = null;
                    String displayFileName = null;
                    String fileType = "application/pdf";
                    Long fileSize = 0L;
                    String localFilePath = null;

                    // STEP 1: ALWAYS download file to uploads folder first
                    logger.info("📥 Step 1: Downloading file to uploads folder...");
                    try {
                        localFilePath = fileStorageService.downloadFileFromUrl(info.getDownloadUrl(), fileName);
                        logger.info("✅✅✅ File downloaded to LOCAL STORAGE: {}", localFilePath);
                        fileSize = fileStorageService.getFileSize(localFilePath);
                        logger.info("📊 File size: {} bytes ({} MB)", fileSize, fileSize / 1024.0 / 1024.0);
                        
                        // Validate file size
                        if (fileSize < 10240) {
                            logger.error("❌ Downloaded file is too small ({} bytes), likely not a valid file.", fileSize);
                            fileStorageService.deleteFile(localFilePath);
                            throw new IOException("Downloaded file is too small (" + fileSize + " bytes).");
                        }
                    } catch (Exception downloadEx) {
                        logger.error("❌ Failed to download file to uploads: {}", downloadEx.getMessage(), downloadEx);
                        throw new IOException("Could not download file: " + downloadEx.getMessage(), downloadEx);
                    }

                    if (driveAvailable) {
                        // STEP 2: Upload file from uploads to Google Drive
                        logger.info("🚀 Step 2: Uploading file from uploads to Google Drive...");
                        try {
                            java.io.File localFile = fileStorageService.getFileStorageLocation().resolve(localFilePath).toFile();
                            try (java.io.FileInputStream fileInputStream = new java.io.FileInputStream(localFile)) {
                                // Extract display filename from localFilePath (remove UUID prefix)
                                String displayName = fileName;
                                if (localFilePath.contains("_") && localFilePath.length() > 40) {
                                    int underscoreIndex = localFilePath.indexOf('_');
                                    if (underscoreIndex > 0) {
                                        displayName = localFilePath.substring(underscoreIndex + 1);
                                    }
                                }
                                
                                String driveFileId = googleDriveService.uploadFile(fileInputStream, displayName, fileType);
                                logger.info("✅✅✅ File uploaded to Google Drive successfully! ✅✅✅");
                                logger.info("📎 Drive File ID: {}", driveFileId);

                                // Get file metadata from Drive
                                File driveFile = googleDriveService.getFileMetadata(driveFileId);
                                fileSize = driveFile.getSize() != null ? driveFile.getSize() : fileSize;
                                logger.info("📊 File size: {} bytes ({} MB)", fileSize, fileSize / 1024.0 / 1024.0);

                                // Validate file size - should be at least 10KB for a real file
                                if (fileSize < 10240) {
                                    logger.error("❌ Uploaded file is too small ({} bytes), likely not a valid file.",
                                            fileSize);
                                    // Delete from Drive if too small
                                    try {
                                        googleDriveService.deleteFile(driveFileId);
                                    } catch (Exception deleteEx) {
                                        logger.warn("⚠️ Failed to delete invalid file from Drive: {}",
                                                deleteEx.getMessage());
                                    }
                                    throw new IOException("Uploaded file is too small (" + fileSize + " bytes).");
                                }

                                // Use Drive file ID as primary storage
                                displayFileName = driveFile.getName();
                                if (fileName != null && !fileName.isEmpty() && fileName.length() > 3
                                        && !fileName.equals("download.pdf")) {
                                    displayFileName = fileName;
                                }
                                filePath = driveFileId; // Store Drive file ID in database
                                fileType = driveFile.getMimeType() != null ? driveFile.getMimeType() : "application/pdf";

                                logger.info("✅✅✅ File saved to Google Drive! ✅✅✅");
                                logger.info("🆔 Drive File ID (stored in database): {}", filePath);
                                logger.info("📄 Display name: {}", displayFileName);
                                logger.info("📊 File size: {} bytes ({} MB)", fileSize, fileSize / 1024.0 / 1024.0);

                                // STEP 3: Delete local file if checkbox is NOT checked
                                // IMPORTANT: File is deleted ONLY AFTER successful upload to Drive
                                if (!downloadFile) {
                                    logger.info("🗑️ Step 3: Checkbox NOT checked - File uploaded to Drive successfully, now deleting from uploads...");
                                    logger.info("⏳ Waiting for upload confirmation before deleting local file...");
                                    boolean deleted = fileStorageService.deleteFile(localFilePath);
                                    if (deleted) {
                                        logger.info("✅✅✅ File deleted from uploads folder (kept only on Drive) ✅✅✅");
                                    } else {
                                        logger.warn("⚠️ Failed to delete file from uploads: {}", localFilePath);
                                    }
                                } else {
                                    logger.info("✅ Step 3: Checkbox checked - File kept in uploads folder");
                                    logger.info("📁 Local file path: {}", localFilePath);
                                }
                            }

                        } catch (Exception driveEx) {
                            logger.error("❌❌❌ Google Drive upload FAILED! ❌❌❌");
                            logger.error("Error message: {}", driveEx.getMessage());
                            if (driveEx.getCause() != null) {
                                logger.error("Error cause: {}", driveEx.getCause().getMessage());
                            }
                            logger.error("Error stack trace:", driveEx);
                            
                            // File already downloaded to uploads, use it as fallback
                            logger.warn("🔄 Drive upload failed - Using local file as fallback...");
                            
                            // Validate file size
                            if (fileSize < 10240) {
                                logger.error("❌ Downloaded file is too small ({} bytes), likely not a valid file.",
                                        fileSize);
                                // Delete invalid file
                                fileStorageService.deleteFile(localFilePath);
                                throw new IOException("Downloaded file is too small (" + fileSize + " bytes).");
                            }

                            // Determine actual filename from saved filename
                            displayFileName = localFilePath;
                            if (localFilePath.contains("_") && localFilePath.length() > 40) {
                                int underscoreIndex = localFilePath.indexOf('_');
                                if (underscoreIndex > 0) {
                                    displayFileName = localFilePath.substring(underscoreIndex + 1);
                                }
                            }

                            if (fileName != null && !fileName.isEmpty() && fileName.length() > 3
                                    && !fileName.equals("download.pdf")) {
                                displayFileName = fileName;
                            }

                            filePath = localFilePath; // Use local path in database
                            fileType = detectFileType(localFilePath);

                            logger.info("💾 File saved to LOCAL STORAGE (Drive upload failed, using local file)");
                            logger.info("📁 Local path: {}", filePath);
                            logger.info("📄 Display name: {}", displayFileName);
                        }
                    } else {
                        // Google Drive is NOT available - file already downloaded in STEP 1
                        logger.warn("⚠️ Google Drive is not available - Using local file from STEP 1");
                        
                        if (downloadFile) {
                            // Checkbox is checked - keep file in uploads
                            logger.info("✅ Checkbox checked - File kept in uploads folder");
                            
                            // Determine actual filename from saved filename
                            displayFileName = localFilePath;
                            if (localFilePath.contains("_") && localFilePath.length() > 40) {
                                int underscoreIndex = localFilePath.indexOf('_');
                                if (underscoreIndex > 0) {
                                    displayFileName = localFilePath.substring(underscoreIndex + 1);
                                }
                            }

                            if (fileName != null && !fileName.isEmpty() && fileName.length() > 3
                                    && !fileName.equals("download.pdf")) {
                                displayFileName = fileName;
                            }

                            filePath = localFilePath; // Use local path in database
                            fileType = detectFileType(localFilePath);

                            logger.info("💾 File saved to LOCAL STORAGE (Drive not available, checkbox checked)");
                            logger.info("📁 Local path: {}", filePath);
                            logger.info("📄 Display name: {}", displayFileName);
                        } else {
                            // Drive not available AND checkbox not checked - delete file and skip
                            logger.warn("⚠️⚠️⚠️ Google Drive not available and checkbox NOT checked - Deleting file from uploads...");
                            boolean deleted = fileStorageService.deleteFile(localFilePath);
                            if (deleted) {
                                logger.info("✅ File deleted from uploads folder");
                            }
                            logger.warn("⚠️⚠️⚠️ This book will be saved WITHOUT the file attachment.");
                            filePath = null;
                        }
                    }

                    // Create and add BookFile only if we successfully got a file path
                    if (filePath != null && !filePath.isEmpty()) {
                        BookFile bookFile = new BookFile();
                        bookFile.setFileName(displayFileName);
                        bookFile.setFilePath(filePath);
                        bookFile.setFileType(fileType);
                        bookFile.setFileSize(fileSize);
                        bookFile.setBook(book);

                        book.getFiles().add(bookFile);
                    }

                } catch (Exception e) {
                    logger.error("❌❌❌ Failed to process file for book '{}': {}", info.getTitle(), e.getMessage(), e);
                    // Continue saving book without file - don't fail entire save
                    logger.error("⚠️ Book will be saved WITHOUT file attachment.");
                }
            }

            Book savedBook = bookRepository.save(book);
            logger.info("SUCCESS: Book saved to database with ID: {}", savedBook.getId());
            return savedBook;

        } catch (Exception e) {
            logger.error("CRITICAL ERROR saving book '{}': {}",
                    info != null ? info.getTitle() : "unknown", e.getMessage(), e);
            throw e; // Re-throw to let caller know save failed
        }
    }

    private String extractFileNameFromUrl(String url) {
        try {
            if (url == null || url.isEmpty()) {
                return null;
            }

            // For Google Drive links, we can't extract filename from URL
            if (url.contains("docs.google.com") || url.contains("drive.google.com")) {
                // Return null to let FileStorageService handle it from Content-Disposition
                // header
                logger.info("Google Drive URL detected, will extract filename from response headers");
                return null;
            }

            // Try to extract from URL path
            String fileName = null;
            try {
                java.net.URL urlObj = new java.net.URL(url);
                String path = urlObj.getPath();
                if (path != null && !path.isEmpty()) {
                    fileName = path.substring(path.lastIndexOf('/') + 1);
                }
            } catch (Exception e) {
                // Fallback: simple string extraction
                int lastSlash = url.lastIndexOf('/');
                if (lastSlash >= 0 && lastSlash < url.length() - 1) {
                    fileName = url.substring(lastSlash + 1);
                }
            }

            if (fileName == null || fileName.isEmpty()) {
                return null;
            }

            // Remove query parameters
            if (fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf('?'));
            }
            // Remove fragment
            if (fileName.contains("#")) {
                fileName = fileName.substring(0, fileName.indexOf('#'));
            }

            // Clean up filename - remove invalid characters
            fileName = fileName.trim();

            // Check if filename looks valid (has extension and reasonable length)
            // Avoid single character or very short names like "uc", "id", etc.
            if (fileName.isEmpty() || fileName.length() < 3 ||
                    (!fileName.contains(".") && fileName.length() < 5) ||
                    fileName.equals("uc") || fileName.equals("id") || fileName.equals("file")) {
                logger.warn("Filename '{}' from URL looks invalid, will use default", fileName);
                return null;
            }

            // Validate it has a proper extension
            if (!fileName.contains(".")) {
                logger.warn("Filename '{}' has no extension, will use default", fileName);
                return null;
            }

            return fileName;
        } catch (Exception e) {
            logger.warn("Could not extract filename from URL: {}", url, e);
            return null; // Return null to let FileStorageService determine filename
        }
    }

    private String detectFileType(String fileName) {
        if (fileName.toLowerCase().endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.toLowerCase().endsWith(".epub")) {
            return "application/epub+zip";
        } else if (fileName.toLowerCase().endsWith(".mobi")) {
            return "application/x-mobipocket-ebook";
        }
        return "application/octet-stream";
    }

    /**
     * Inner class to hold book information during crawling
     */
    public static class BookInfo {
        private String title;
        private String author;
        private String description;
        private String imageUrl;
        private String detailUrl;
        private String downloadUrl;
        private Set<String> categories = new HashSet<>();

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getDetailUrl() {
            return detailUrl;
        }

        public void setDetailUrl(String detailUrl) {
            this.detailUrl = detailUrl;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }

        public Set<String> getCategories() {
            return categories;
        }

        public void addCategory(String category) {
            this.categories.add(category);
        }
    }

    /**
     * Result class for crawl operations
     */
    public static class CrawlResult {
        private int successCount = 0;
        private int failedCount = 0;
        private int skippedCount = 0;
        private long startTime;
        private long endTime;
        private long duration;
        private List<String> errors = new ArrayList<>();

        public void incrementSuccess() {
            successCount++;
        }

        public void incrementFailed() {
            failedCount++;
        }

        public void incrementSkipped() {
            skippedCount++;
        }

        public void addError(String error) {
            errors.add(error);
        }

        // Getters and setters
        public int getSuccessCount() {
            return successCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public void setEndTime(long endTime) {
            this.endTime = endTime;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}