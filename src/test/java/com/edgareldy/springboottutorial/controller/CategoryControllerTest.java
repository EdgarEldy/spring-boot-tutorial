package com.edgareldy.springboottutorial.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgareldy.springboottutorial.dto.category.CategoryRequest;
import com.edgareldy.springboottutorial.dto.category.CategoryResponse;
import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.service.CategoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc integration tests for {@link CategoryController}, with
 * {@link CategoryService} mocked. Security filters are disabled since
 * {@code feature/auth} has not defined a {@code SecurityConfig} yet.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void _01_ShouldReturnPagedCategories_WhenListingCategories() throws Exception {
        CategoryResponse category = new CategoryResponse(1L, "Electronics", null, null, List.of());
        PageResponse<CategoryResponse> page = new PageResponse<>(List.of(category), 0, 20, 1, 1);
        when(categoryService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].category_name").value("Electronics"));
    }

    @Test
    void _02_ShouldReturnCategory_WhenCategoryFound() throws Exception {
        when(categoryService.findById(1L)).thenReturn(new CategoryResponse(1L, "Electronics", null, null, List.of()));

        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category_name").value("Electronics"));
    }

    @Test
    void _03_ShouldIncludeProducts_WhenFindingCategoryById() throws Exception {
        ProductResponse keyboard = new ProductResponse(10L, "Keyboard", 79.99f, 1L, "Electronics");
        when(categoryService.findById(1L))
                .thenReturn(new CategoryResponse(1L, "Electronics", null, null, List.of(keyboard)));

        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.products[0].product_name").value("Keyboard"));
    }

    @Test
    void _04_ShouldLeaveProductsEmpty_WhenListingCategories() throws Exception {
        CategoryResponse category = new CategoryResponse(1L, "Electronics", null, null, List.of());
        when(categoryService.findAll(any())).thenReturn(new PageResponse<>(List.of(category), 0, 20, 1, 1));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].products").isEmpty());
    }

    @Test
    void _05_ShouldReturn404_WhenCategoryMissing() throws Exception {
        when(categoryService.findById(eq(99L)))
                .thenThrow(new ResourceNotFoundException("Category not found with id 99"));

        mockMvc.perform(get("/api/v1/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _06_ShouldReturn201_WhenCreateRequestValid() throws Exception {
        when(categoryService.create(any())).thenReturn(new CategoryResponse(2L, "Books", null, null, List.of()));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Books"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.category_name").value("Books"));
    }

    @Test
    void _07_ShouldReturn400_WhenCategoryNameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _08_ShouldReturn200_WhenUpdateRequestValid() throws Exception {
        when(categoryService.update(eq(1L), any())).thenReturn(new CategoryResponse(1L, "Home Appliances", null, null, List.of()));

        mockMvc.perform(put("/api/v1/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Home Appliances"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category_name").value("Home Appliances"));
    }

    @Test
    void _09_ShouldReturn404_WhenCategoryMissingOnUpdate() throws Exception {
        when(categoryService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Category not found with id 99"));

        mockMvc.perform(put("/api/v1/categories/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Home Appliances"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _10_ShouldReturn200_WhenDeleteSucceeds() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void _11_ShouldReturn404_WhenCategoryMissingOnDelete() throws Exception {
        doThrow(new ResourceNotFoundException("Category not found with id 99"))
                .when(categoryService).delete(99L);

        mockMvc.perform(delete("/api/v1/categories/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _12_ShouldReturn422_WhenCategoryHasProducts() throws Exception {
        doThrow(new BusinessRuleException("Category with id 1 still has products and cannot be deleted"))
                .when(categoryService).delete(1L);

        mockMvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }
}
