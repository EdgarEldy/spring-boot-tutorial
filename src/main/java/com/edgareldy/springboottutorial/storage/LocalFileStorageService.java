package com.edgareldy.springboottutorial.storage;

import com.edgareldy.springboottutorial.config.StorageProperties;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link FileStorageService} implementation that writes files to a local
 * directory on the server's filesystem. The upload root is configured via
 * {@link StorageProperties#uploadDir()}. Files are named with a UUID to avoid
 * collisions and path-traversal attacks; the original filename is never used
 * in the stored path. Acceptable formats are limited to JPEG, PNG, and WebP.
 * Files larger than {@link StorageProperties#maxFileSizeMb()} are rejected.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private final StorageProperties storageProperties;

    public LocalFileStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public String store(MultipartFile file, String subdirectory) {
        validateFile(file);

        String extension = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "." + extension;

        Path uploadRoot = Paths.get(storageProperties.uploadDir()).toAbsolutePath().normalize();
        Path targetDir = uploadRoot.resolve(subdirectory).normalize();

        // Guard against path traversal via the subdirectory parameter
        if (!targetDir.startsWith(uploadRoot)) {
            throw new BusinessRuleException("Invalid storage subdirectory");
        }

        try {
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetFile);
        } catch (IOException e) {
            throw new BusinessRuleException("Failed to store file: " + e.getMessage());
        }

        return "/uploads/" + subdirectory + "/" + filename;
    }

    @Override
    public void delete(String urlPath) {
        if (urlPath == null || !urlPath.startsWith("/uploads/")) {
            return;
        }
        String relativePath = urlPath.substring("/uploads/".length());
        Path uploadRoot = Paths.get(storageProperties.uploadDir()).toAbsolutePath().normalize();
        Path target = uploadRoot.resolve(relativePath).normalize();

        // Only delete files that live under the configured upload root
        if (!target.startsWith(uploadRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            // Silently ignore deletion failures
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("File must not be empty");
        }

        long maxBytes = (long) storageProperties.maxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessRuleException(
                    "File size exceeds the maximum allowed size of "
                    + storageProperties.maxFileSizeMb() + " MB");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessRuleException(
                    "File type not allowed. Accepted types: jpg, jpeg, png, webp");
        }

        String contentType = resolveContentType(file);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessRuleException(
                    "File MIME type not allowed. Accepted types: image/jpeg, image/png, image/webp");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String resolveContentType(MultipartFile file) {
        String declared = file.getContentType();
        if (declared != null && !declared.isBlank()) {
            return declared.toLowerCase();
        }
        // Fall back to probing when the browser does not declare a content type
        String probed = URLConnection.guessContentTypeFromName(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        return probed != null ? probed.toLowerCase() : "";
    }
}
