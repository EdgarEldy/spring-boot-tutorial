package edgareldy.springboottutorial.controller;

import edgareldy.springboottutorial.dto.category.CategoryRequest;
import edgareldy.springboottutorial.dto.category.CategoryResponse;
import edgareldy.springboottutorial.dto.common.ApiResponse;
import edgareldy.springboottutorial.dto.common.PageResponse;
import edgareldy.springboottutorial.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing CRUD endpoints for categories. Delegates every
 * operation to {@link CategoryService}; no business logic lives here.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List categories, paginated")
    public ApiResponse<PageResponse<CategoryResponse>> findAll(Pageable pageable) {
        return ApiResponse.success(categoryService.findAll(pageable), "Categories retrieved successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a category by id")
    public ApiResponse<CategoryResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(categoryService.findById(id), "Category retrieved successfully");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(categoryService.create(request), "Category created successfully");
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success(categoryService.update(id, request), "Category updated successfully");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success(null, "Category deleted successfully");
    }
}
