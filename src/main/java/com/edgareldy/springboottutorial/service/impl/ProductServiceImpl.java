package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.product.ProductRequest;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import com.edgareldy.springboottutorial.entity.Category;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.mapper.ProductMapper;
import com.edgareldy.springboottutorial.repository.CategoryRepository;
import com.edgareldy.springboottutorial.repository.OrderRepository;
import com.edgareldy.springboottutorial.repository.ProductRepository;
import com.edgareldy.springboottutorial.repository.specification.ProductSpecifications;
import com.edgareldy.springboottutorial.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link ProductService} implementation backed by
 * {@link ProductRepository}. {@code findById} is cached ("products", keyed
 * by id, see {@code CacheConfig}), evicted on {@code update}/{@code delete},
 * same reasoning as {@code CategoryServiceImpl}. Note this does not evict a
 * cached product when its category is renamed elsewhere: the denormalized
 * category name in a cached {@code ProductResponse} can lag behind for up
 * to the cache's TTL, an accepted simplification for this tutorial rather
 * than a cross-cache invalidation mechanism. {@code findAll} falls back to
 * a dynamic {@link Specification} (see {@link ProductSpecifications}) when
 * a caller supplies {@code productName}/{@code minPrice}/{@code maxPrice},
 * keeping the plain and {@code categoryId}-only paths on their existing,
 * {@code @EntityGraph}-optimized repository methods.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final OrderRepository orderRepository;

    @Override
    public PageResponse<ProductResponse> findAll(
            Long categoryId, String productName, Float minPrice, Float maxPrice, Pageable pageable) {
        Page<Product> page;
        if (productName != null || minPrice != null || maxPrice != null) {
            // Falls through to the Specification-based query, which does not
            // benefit from the @EntityGraph on findAll/findByCategoryId below:
            // an accepted N+1 risk on this path, kept simple rather than
            // complicating ProductSpecifications with a fetch join.
            Specification<Product> specification = Specification.allOf(
                    ProductSpecifications.hasCategoryId(categoryId),
                    ProductSpecifications.nameContains(productName),
                    ProductSpecifications.priceBetween(minPrice, maxPrice));
            page = productRepository.findAll(specification, pageable);
        } else if (categoryId != null) {
            page = productRepository.findByCategoryId(categoryId, pageable);
        } else {
            page = productRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(productMapper::toResponse));
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductResponse findById(Long id) {
        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = getCategoryOrThrow(request.categoryId());
        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
        Category category = getCategoryOrThrow(request.categoryId());
        productMapper.updateEntityFromRequest(request, product);
        product.setCategory(category);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id " + id);
        }
        if (orderRepository.existsByProductId(id)) {
            throw new BusinessRuleException("Product " + id + " has existing orders and cannot be deleted");
        }
        productRepository.deleteById(id);
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + categoryId));
    }
}
