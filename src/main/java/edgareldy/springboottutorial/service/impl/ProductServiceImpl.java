package edgareldy.springboottutorial.service.impl;

import edgareldy.springboottutorial.dto.common.PageResponse;
import edgareldy.springboottutorial.dto.product.ProductRequest;
import edgareldy.springboottutorial.dto.product.ProductResponse;
import edgareldy.springboottutorial.entity.Category;
import edgareldy.springboottutorial.entity.Product;
import edgareldy.springboottutorial.exception.BusinessRuleException;
import edgareldy.springboottutorial.exception.ResourceNotFoundException;
import edgareldy.springboottutorial.mapper.ProductMapper;
import edgareldy.springboottutorial.repository.CategoryRepository;
import edgareldy.springboottutorial.repository.OrderRepository;
import edgareldy.springboottutorial.repository.ProductRepository;
import edgareldy.springboottutorial.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
 * than a cross-cache invalidation mechanism.
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
    public PageResponse<ProductResponse> findAll(Long categoryId, Pageable pageable) {
        Page<Product> page = categoryId != null
                ? productRepository.findByCategoryId(categoryId, pageable)
                : productRepository.findAll(pageable);
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
