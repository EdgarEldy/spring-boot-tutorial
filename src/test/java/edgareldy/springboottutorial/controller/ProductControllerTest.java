package edgareldy.springboottutorial.controller;

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
import edgareldy.springboottutorial.dto.common.PageResponse;
import edgareldy.springboottutorial.dto.product.ProductRequest;
import edgareldy.springboottutorial.dto.product.ProductResponse;
import edgareldy.springboottutorial.exception.ResourceNotFoundException;
import edgareldy.springboottutorial.security.JwtService;
import edgareldy.springboottutorial.service.ProductService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

    @Test
    void findAllWithoutCategoryIdPassesNullThrough() throws Exception {
        ProductResponse product = new ProductResponse(1L, "Keyboard", 79.99f, 1L, "Electronics");
        PageResponse<ProductResponse> page = new PageResponse<>(List.of(product), 0, 20, 1, 1);
        when(productService.findAll(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].productName").value("Keyboard"));
    }

    @Test
    void findAllWithCategoryIdForwardsFilter() throws Exception {
        ProductResponse product = new ProductResponse(1L, "Keyboard", 79.99f, 1L, "Electronics");
        PageResponse<ProductResponse> page = new PageResponse<>(List.of(product), 0, 20, 1, 1);
        when(productService.findAll(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/products").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].categoryId").value(1));
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        when(productService.findById(99L)).thenThrow(new ResourceNotFoundException("Product not found with id 99"));

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createReturns201WhenValid() throws Exception {
        ProductRequest request = new ProductRequest(1L, "Keyboard", 79.99f);
        when(productService.create(any())).thenReturn(new ProductResponse(1L, "Keyboard", 79.99f, 1L, "Electronics"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.productName").value("Keyboard"));
    }

    @Test
    void createReturns400WhenUnitPriceNotPositive() throws Exception {
        ProductRequest invalid = new ProductRequest(1L, "Keyboard", -5f);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createReturns404WhenCategoryMissing() throws Exception {
        ProductRequest request = new ProductRequest(99L, "Keyboard", 79.99f);
        when(productService.create(any())).thenThrow(new ResourceNotFoundException("Category not found with id 99"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturns200WhenValid() throws Exception {
        ProductRequest request = new ProductRequest(1L, "Mechanical Keyboard", 99.99f);
        when(productService.update(eq(1L), any()))
                .thenReturn(new ProductResponse(1L, "Mechanical Keyboard", 99.99f, 1L, "Electronics"));

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productName").value("Mechanical Keyboard"));
    }

    @Test
    void updateReturns404WhenProductMissing() throws Exception {
        ProductRequest request = new ProductRequest(1L, "Mechanical Keyboard", 99.99f);
        when(productService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Product not found with id 99"));

        mockMvc.perform(put("/api/v1/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns200WhenSuccessful() throws Exception {
        mockMvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found with id 99"))
                .when(productService).delete(99L);

        mockMvc.perform(delete("/api/v1/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
