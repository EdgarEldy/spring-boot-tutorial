package edgareldy.springboottutorial.service;

import edgareldy.springboottutorial.dto.category.CategoryRequest;
import edgareldy.springboottutorial.dto.category.CategoryResponse;
import edgareldy.springboottutorial.dto.common.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Contract for {@link edgareldy.springboottutorial.entity.Category} business
 * operations. Controllers and tests depend on this interface, never on its
 * implementation directly.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public interface CategoryService {

    PageResponse<CategoryResponse> findAll(Pageable pageable);

    CategoryResponse findById(Long id);

    CategoryResponse create(CategoryRequest request);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);
}
