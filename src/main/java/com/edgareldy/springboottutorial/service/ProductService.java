package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.product.ProductRequest;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import org.springframework.data.domain.Pageable;

/**
 * Contract for {@link com.edgareldy.springboottutorial.entity.Product} business
 * operations. Controllers and tests depend on this interface, never on its
 * implementation directly.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public interface ProductService {

    PageResponse<ProductResponse> findAll(
            Long categoryId, String productName, Float minPrice, Float maxPrice, Pageable pageable);

    ProductResponse findById(Long id);

    ProductResponse create(ProductRequest request);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);
}
