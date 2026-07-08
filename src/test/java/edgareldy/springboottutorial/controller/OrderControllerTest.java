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
import edgareldy.springboottutorial.dto.order.OrderRequest;
import edgareldy.springboottutorial.dto.order.OrderResponse;
import edgareldy.springboottutorial.exception.ResourceNotFoundException;
import edgareldy.springboottutorial.security.JwtService;
import edgareldy.springboottutorial.service.OrderService;
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
 * MockMvc integration tests for {@link OrderController}, with
 * {@link OrderService} mocked. Security filters are disabled: this
 * endpoint's authorization rules (authenticated GET, ADMIN-only writes) are
 * SecurityConfig's responsibility, not this controller's, and are covered
 * separately.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    // Unused directly: JwtAuthFilter (a Filter, auto-included by @WebMvcTest
    // regardless of addFilters) needs these to construct, even though the
    // filter chain never actually runs in this test.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static OrderResponse sampleResponse() {
        return new OrderResponse(
                1L,
                new OrderResponse.CustomerSummary(1L, "Ada Lovelace"),
                new OrderResponse.ProductSummary(1L, "Keyboard", 50.0f),
                2,
                100.0);
    }

    @Test
    void findAllWithoutFiltersPassesNullsThrough() throws Exception {
        PageResponse<OrderResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(orderService.findAll(isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].total").value(100.0));
    }

    @Test
    void findAllForwardsCustomerAndProductFilters() throws Exception {
        PageResponse<OrderResponse> page = new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
        when(orderService.findAll(eq(1L), eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/orders").param("customerId", "1").param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].customer.fullName").value("Ada Lovelace"));
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        when(orderService.findById(99L)).thenThrow(new ResourceNotFoundException("Order not found with id 99"));

        mockMvc.perform(get("/api/v1/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createReturns201WhenValid() throws Exception {
        when(orderService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(1L, 1L, 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.product.productName").value("Keyboard"));
    }

    @Test
    void createReturns400WhenQuantityNotPositive() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(1L, 1L, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createReturns404WhenCustomerMissing() throws Exception {
        when(orderService.create(any())).thenThrow(new ResourceNotFoundException("Customer not found with id 99"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(99L, 1L, 2))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturns200WhenValid() throws Exception {
        when(orderService.update(eq(1L), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(1L, 1L, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(2));
    }

    @Test
    void updateReturns404WhenOrderMissing() throws Exception {
        when(orderService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Order not found with id 99"));

        mockMvc.perform(put("/api/v1/orders/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(1L, 1L, 2))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns200WhenSuccessful() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        doThrow(new ResourceNotFoundException("Order not found with id 99"))
                .when(orderService).delete(99L);

        mockMvc.perform(delete("/api/v1/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
