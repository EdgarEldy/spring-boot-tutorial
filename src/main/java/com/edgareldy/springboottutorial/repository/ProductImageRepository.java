package com.edgareldy.springboottutorial.repository;

import com.edgareldy.springboottutorial.entity.ProductImage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link ProductImage}. Provides derived queries
 * for the two most common access patterns: loading all images of a product
 * (ordered by display order) and finding the primary image of a product.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    Optional<ProductImage> findByProductIdAndPrimaryTrue(Long productId);
}
