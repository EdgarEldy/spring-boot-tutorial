package com.edgareldy.springboottutorial.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.product.ProductImageResponse;
import com.edgareldy.springboottutorial.dto.product.ProductRequest;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.security.JwtService;
import com.edgareldy.springboottutorial.service.BlacklistedTokenService;
import com.edgareldy.springboottutorial.service.ProductImageService;
import com.edgareldy.springboottutorial.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/**
 * MockMvc integration tests for {@link ProductController}, with
 * {@link ProductService} mocked. Security filters are disabled: this
 * endpoint's authorization rules (public GET, ADMIN-only writes) are
 * SecurityConfig's responsibility, not this controller's, and are covered
 * separately.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    // Unused directly: JwtAuthFilter (a Filter, auto-included by @WebMvcTest
    // regardless of addFilters) needs these to construct, even though the
    // filter chain never actually runs in this test.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private BlacklistedTokenService blacklistedTokenService;

    @MockitoBean
    private ProductImageService productImageService;

    @Test
    void _01_ShouldPassNullCategoryId_WhenNoFilterGiven() throws Exception {
        ProductResponse product =
                new ProductResponse(1L, "Keyboard", new BigDecimal("79.99"), null, true, 1L, "Electronics", List.of());
        PageResponse<ProductResponse> page = new PageResponse<>(List.of(product), 0, 20, 1, 1);
        when(productService.findAll(isNull(), isNull(), isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].product_name").value("Keyboard"));
    }

    @Test
    void _02_ShouldForwardCategoryFilter_WhenCategoryIdGiven() throws Exception {
        ProductResponse product =
                new ProductResponse(1L, "Keyboard", new BigDecimal("79.99"), null, true, 1L, "Electronics", List.of());
        PageResponse<ProductResponse> page = new PageResponse<>(List.of(product), 0, 20, 1, 1);
        when(productService.findAll(eq(1L), isNull(), isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/products").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].category_id").value(1));
    }

    @Test
    void _03_ShouldForwardNameAndPriceFilters_WhenAdvancedFiltersGiven() throws Exception {
        ProductResponse product =
                new ProductResponse(1L, "Keyboard", new BigDecimal("79.99"), null, true, 1L, "Electronics", List.of());
        PageResponse<ProductResponse> page = new PageResponse<>(List.of(product), 0, 20, 1, 1);
        when(productService.findAll(isNull(), eq("key"), eq(new BigDecimal("50")), eq(new BigDecimal("100")), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/products")
                        .param("productName", "key")
                        .param("minPrice", "50")
                        .param("maxPrice", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].product_name").value("Keyboard"));
    }

    @Test
    void _04_ShouldReturn404_WhenProductMissing() throws Exception {
        when(productService.findById(99L)).thenThrow(new ResourceNotFoundException("Product not found with id 99"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _05_ShouldReturn201_WhenCreateRequestValid() throws Exception {
        ProductRequest request = new ProductRequest(1L, "Keyboard", null, new BigDecimal("79.99"));
        when(productService.create(any()))
                .thenReturn(new ProductResponse(1L, "Keyboard", new BigDecimal("79.99"), null, true, 1L, "Electronics", List.of()));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.product_name").value("Keyboard"));
    }

    @Test
    void _06_ShouldReturn400_WhenUnitPriceNotPositive() throws Exception {
        ProductRequest invalid = new ProductRequest(1L, "Keyboard", null, new BigDecimal("-5"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _07_ShouldReturn404_WhenCategoryMissingOnCreate() throws Exception {
        ProductRequest request = new ProductRequest(99L, "Keyboard", null, new BigDecimal("79.99"));
        when(productService.create(any())).thenThrow(new ResourceNotFoundException("Category not found with id 99"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void _08_ShouldReturn200_WhenUpdateRequestValid() throws Exception {
        ProductRequest request = new ProductRequest(1L, "Mechanical Keyboard", null, new BigDecimal("99.99"));
        when(productService.update(eq(1L), any()))
                .thenReturn(new ProductResponse(
                        1L, "Mechanical Keyboard", new BigDecimal("99.99"), null, true, 1L, "Electronics", List.of()));

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.product_name").value("Mechanical Keyboard"));
    }

    @Test
    void _09_ShouldReturn409_WhenProductIsStale() throws Exception {
        ProductRequest request = new ProductRequest(1L, "Mechanical Keyboard", null, new BigDecimal("99.99"));
        when(productService.update(eq(1L), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(Object.class, 1L));

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _10_ShouldReturn404_WhenProductMissingOnUpdate() throws Exception {
        ProductRequest request = new ProductRequest(1L, "Mechanical Keyboard", null, new BigDecimal("99.99"));
        when(productService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Product not found with id 99"));

        mockMvc.perform(put("/api/v1/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void _11_ShouldReturn200_WhenDeleteSucceeds() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void _12_ShouldReturn404_WhenProductMissingOnDelete() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found with id 99"))
                .when(productService).delete(99L);

        mockMvc.perform(delete("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _13_ShouldSerializeActiveFalse_WhenProductIsInactive() throws Exception {
        when(productService.findById(2L))
                .thenReturn(new ProductResponse(2L, "Legacy Keyboard", new BigDecimal("49.99"), "Discontinued model", false, 1L, "Electronics", List.of()));

        mockMvc.perform(get("/api/v1/products/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void _14_ShouldReturn201_WhenImageUploaded() throws Exception {
        ProductImageResponse imageResponse = new ProductImageResponse(1L, "/uploads/products/uuid.jpg", null, true, 0);
        when(productImageService.addImage(eq(1L), any(), any(), eq(true))).thenReturn(imageResponse);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "jpg-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/products/1/images")
                        .file(file)
                        .param("isPrimary", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.is_primary").value(true));
    }

    @Test
    void _15_ShouldReturn200_WhenImageDeleted() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1/images/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void _16_ShouldReturn404_WhenProductNotFoundOnAddImage() throws Exception {
        when(productImageService.addImage(eq(99L), any(), any(), eq(false)))
                .thenThrow(new ResourceNotFoundException("Product not found with id 99"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "jpg-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/products/99/images").file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _17_ShouldReturn422_WhenImageFileInvalidOnAdd() throws Exception {
        when(productImageService.addImage(eq(1L), any(), any(), eq(false)))
                .thenThrow(new BusinessRuleException("File type not allowed"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "jpg-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/products/1/images").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _18_ShouldReturn404_WhenImageNotFoundOnDelete() throws Exception {
        doThrow(new ResourceNotFoundException("Image not found with id 99"))
                .when(productImageService).deleteImage(1L, 99L);

        mockMvc.perform(delete("/api/v1/products/1/images/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
