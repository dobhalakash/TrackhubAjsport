package com.dreamnest.controller;

import com.dreamnest.dto.response.ApiResponse;
import com.dreamnest.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles image uploads for product listings, as an alternative to pasting
 * an external image URL. Uploaded files are saved under
 * {@code app.upload.dir} and served back as static resources (see
 * {@link com.dreamnest.config.WebConfig}), so the URL returned here can be
 * used exactly like any other image URL elsewhere in the app.
 */
@RestController
@RequestMapping("/uploads")
public class FileUploadController {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5MB

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"
    );
    private static final long MAX_DOCUMENT_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.public-url}")
    private String publicBaseUrl;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * Uploads a single image and returns its publicly-accessible URL.
     * Business admins use this from the product form's "Upload Image" tab
     * as an alternative to the "Image URL" tab.
     */
    @PostMapping("/image")
    public ApiResponse<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("Image must be smaller than 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Only JPEG, PNG, WEBP, or GIF images are allowed");
        }

        try {
            Path productImagesDir = Paths.get(uploadDir, "products");
            Files.createDirectories(productImagesDir);

            String extension = extensionFor(contentType);
            String filename = UUID.randomUUID() + extension;
            Path target = productImagesDir.resolve(filename);

            // copy via a fresh input stream from the multipart file (transferTo
            // requires an absolute path in some servlet containers, this is safer)
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String publicUrl = publicBaseUrl + contextPath + "/uploads/products/" + filename;
            return ApiResponse.success(Map.of("url", publicUrl));
        } catch (IOException e) {
            throw new BadRequestException("Could not save the uploaded image. Please try again.");
        }
    }

    /**
     * Uploads a general document/image attachment (used by the Support Chat
     * feature for business <-> admin file sharing). Broader file types and a
     * larger size limit than product images, since these can be invoices,
     * screenshots, ID proofs, etc.
     */
    @PostMapping("/document")
    public ApiResponse<Map<String, String>> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded");
        }
        if (file.getSize() > MAX_DOCUMENT_SIZE_BYTES) {
            throw new BadRequestException("File must be smaller than 10MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_DOCUMENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported file type. Allowed: PDF, Word, Excel, images, or plain text.");
        }

        try {
            Path documentsDir = Paths.get(uploadDir, "documents");
            Files.createDirectories(documentsDir);

            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.')) : "";
            String filename = UUID.randomUUID() + extension;
            Path target = documentsDir.resolve(filename);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String publicUrl = publicBaseUrl + contextPath + "/uploads/documents/" + filename;
            Map<String, String> result = new java.util.HashMap<>();
            result.put("url", publicUrl);
            result.put("filename", originalName);
            return ApiResponse.success(result);
        } catch (IOException e) {
            throw new BadRequestException("Could not save the uploaded file. Please try again.");
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
