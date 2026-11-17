package com.edgareldy.springboottutorial.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over the file storage backend. The default implementation
 * writes files to a local directory; a future implementation could delegate
 * to an object-storage service (S3, GCS, etc.) without touching the callers.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
public interface FileStorageService {

    /**
     * Validates and stores the uploaded file, returning its public URL path
     * (e.g. {@code /uploads/products/550e8400-e29b-41d4-a716-446655440000.jpg}).
     *
     * @param file         the uploaded file
     * @param subdirectory target subdirectory under the configured upload root
     * @return the public URL path of the stored file
     * @throws com.edgareldy.springboottutorial.exception.BusinessRuleException
     *         if the file type or size is not acceptable
     */
    String store(MultipartFile file, String subdirectory);

    /**
     * Deletes the file identified by its public URL path. Silently ignores
     * paths that do not map to an existing file.
     *
     * @param urlPath the URL path returned by {@link #store}
     */
    void delete(String urlPath);
}
