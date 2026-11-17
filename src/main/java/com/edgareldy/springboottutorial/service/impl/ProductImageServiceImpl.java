package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.dto.product.ProductImageResponse;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.entity.ProductImage;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.mapper.ProductImageMapper;
import com.edgareldy.springboottutorial.repository.ProductImageRepository;
import com.edgareldy.springboottutorial.repository.ProductRepository;
import com.edgareldy.springboottutorial.service.ProductImageService;
import com.edgareldy.springboottutorial.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Default {@link ProductImageService} implementation. File I/O is delegated to
 * {@link FileStorageService}, keeping this class free of storage concerns.
 * When {@code isPrimary} is true, any existing primary image for the same
 * product is cleared first within the same transaction, upholding the
 * partial unique index on {@code (product_id) WHERE is_primary = TRUE}.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class ProductImageServiceImpl implements ProductImageService {

    private static final String PRODUCTS_SUBDIR = "products";

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final FileStorageService fileStorageService;
    private final ProductImageMapper productImageMapper;

    @Override
    @Transactional
    public ProductImageResponse addImage(Long productId, MultipartFile file, String altText, boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));

        if (isPrimary) {
            productImageRepository.findByProductIdAndPrimaryTrue(productId)
                    .ifPresent(existing -> {
                        existing.setPrimary(false);
                        productImageRepository.save(existing);
                    });
        }

        String imageUrl = fileStorageService.store(file, PRODUCTS_SUBDIR);
        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl)
                .altText(altText)
                .primary(isPrimary)
                .displayOrder(0)
                .build();
        return productImageMapper.toResponse(productImageRepository.save(image));
    }

    @Override
    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id " + imageId));
        if (!image.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Image " + imageId + " does not belong to product " + productId);
        }
        fileStorageService.delete(image.getImageUrl());
        productImageRepository.delete(image);
    }
}
