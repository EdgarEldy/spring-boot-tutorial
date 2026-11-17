package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.dto.product.ProductImageResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Contract for product image operations. Callers supply a product id and an
 * image file; this service validates ownership, delegates file I/O to
 * {@link com.edgareldy.springboottutorial.storage.FileStorageService}, and
 * enforces the "at most one primary image per product" invariant.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
public interface ProductImageService {

    ProductImageResponse addImage(Long productId, MultipartFile file, String altText, boolean isPrimary);

    void deleteImage(Long productId, Long imageId);
}
