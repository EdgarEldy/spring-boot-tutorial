package edgareldy.springboottutorial.service.impl;

import edgareldy.springboottutorial.dto.category.CategoryRequest;
import edgareldy.springboottutorial.dto.category.CategoryResponse;
import edgareldy.springboottutorial.dto.common.PageResponse;
import edgareldy.springboottutorial.entity.Category;
import edgareldy.springboottutorial.exception.BusinessRuleException;
import edgareldy.springboottutorial.exception.ResourceNotFoundException;
import edgareldy.springboottutorial.mapper.CategoryMapper;
import edgareldy.springboottutorial.repository.CategoryRepository;
import edgareldy.springboottutorial.repository.ProductRepository;
import edgareldy.springboottutorial.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link CategoryService} implementation backed by
 * {@link CategoryRepository}. {@code findById} is cached ("categories",
 * keyed by id, see {@code CacheConfig}) since categories change rarely
 * compared to how often they are read; {@code update}/{@code delete} evict
 * the entry for that id so a write is never followed by a stale read.
 * {@code findAll} is intentionally not cached: caching a paginated,
 * sortable collection would need a cache entry per distinct
 * page/size/sort combination, which defeats the point of a simple
 * single-key cache.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public PageResponse<CategoryResponse> findAll(Pageable pageable) {
        Page<CategoryResponse> page = categoryRepository.findAll(pageable)
                .map(categoryMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Cacheable(value = "categories", key = "#id")
    public CategoryResponse findById(Long id) {
        return categoryMapper.toResponse(getCategoryOrThrow(id));
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category category = categoryMapper.toEntity(request);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", key = "#id")
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategoryOrThrow(id);
        categoryMapper.updateEntityFromRequest(request, category);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", key = "#id")
    public void delete(Long id) {
        Category category = getCategoryOrThrow(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException(
                    "Category with id " + id + " still has products and cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    private Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + id));
    }
}
