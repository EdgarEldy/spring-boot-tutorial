package com.edgareldy.springboottutorial.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.order.OrderCustomerResponse;
import com.edgareldy.springboottutorial.dto.order.OrderItemResponse;
import com.edgareldy.springboottutorial.dto.order.OrderRequest;
import com.edgareldy.springboottutorial.dto.order.OrderResponse;
import com.edgareldy.springboottutorial.dto.order.OrderStatusUpdateRequest;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import com.edgareldy.springboottutorial.entity.OrderStatus;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.security.JwtService;
import com.edgareldy.springboottutorial.service.BlacklistedTokenService;
import com.edgareldy.springboottutorial.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc integration tests for {@link OrderController}, with
 * {@link OrderService} mocked. Security filters are disabled: authorization
 * rules (authenticated GET, ADMIN-only writes, ownership checks) are covered
 * by {@code OrderAuthorizationTest} with a real security stack.
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

    // Unused directly: JwtAuthFilter needs these to construct even when addFilters=false.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private BlacklistedTokenService blacklistedTokenService;

    private static OrderCustomerResponse sampleCustomer() {
        return new OrderCustomerResponse(
                1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way");
    }

    private static OrderResponse sampleResponse() {
        ProductResponse product = new ProductResponse(
                1L, "Keyboard", new BigDecimal("50.00"), null, true, 1L, "Electronics", List.of());
        return new OrderResponse(1L, sampleCustomer(), product, 2,
                "ORD-TEST-0001", "PENDING", new BigDecimal("100.00"), List.of());
    }

    private static OrderResponse sampleCheckoutResponse() {
        OrderItemResponse item = new OrderItemResponse(
                1L, 1L, "Keyboard", new BigDecimal("50.00"), 2, new BigDecimal("100.00"));
        return new OrderResponse(1L, null, null, 2,
                "ORD-TEST-0001", "PENDING", new BigDecimal("100.00"), List.of(item));
    }

    private static PageResponse<OrderResponse> samplePage() {
        return new PageResponse<>(List.of(sampleResponse()), 0, 20, 1, 1);
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void _01_ShouldReturnUserOrders_WhenAuthenticatedAsUser() throws Exception {
        when(orderService.findAll(eq("alice"), eq(false), any())).thenReturn(samplePage());

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].order_number").value("ORD-TEST-0001"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void _02_ShouldReturnAllOrders_WhenAuthenticatedAsAdmin() throws Exception {
        when(orderService.findAll(eq("admin"), eq(true), any())).thenReturn(samplePage());

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].order_number").value("ORD-TEST-0001"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void _03_ShouldReturn200_WhenOrderFound() throws Exception {
        when(orderService.findById(eq(1L), eq("admin"), eq(true))).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order_number").value("ORD-TEST-0001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.total_amount").value(100.00))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void _03b_ShouldReturn404_WhenOrderMissing() throws Exception {
        when(orderService.findById(eq(99L), eq("alice"), eq(false)))
                .thenThrow(new ResourceNotFoundException("Order not found with id 99"));

        mockMvc.perform(get("/api/v1/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void _04_ShouldReturn201_WhenCheckoutSucceeds() throws Exception {
        when(orderService.checkout("alice")).thenReturn(sampleCheckoutResponse());

        mockMvc.perform(post("/api/v1/orders"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.order_number").value("ORD-TEST-0001"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.total_amount").value(100.00))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].product_name").value("Keyboard"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void _05_ShouldReturn422_WhenCartIsEmpty() throws Exception {
        when(orderService.checkout("alice")).thenThrow(new BusinessRuleException("Cart is empty"));

        mockMvc.perform(post("/api/v1/orders"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "alice", roles = "USER")
    void _06_ShouldReturn404_WhenCartNotFound() throws Exception {
        when(orderService.checkout("alice"))
                .thenThrow(new ResourceNotFoundException("Cart not found for user: alice"));

        mockMvc.perform(post("/api/v1/orders"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void _07_ShouldReturn200_WhenUpdateRequestValid() throws Exception {
        when(orderService.update(eq(1L), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(1L, 1L, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(2));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void _08_ShouldReturn404_WhenOrderMissingOnUpdate() throws Exception {
        when(orderService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Order not found with id 99"));

        mockMvc.perform(put("/api/v1/orders/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(1L, 1L, 2))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void _09_ShouldReturn200_WhenDeleteSucceeds() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void _10_ShouldReturn404_WhenOrderMissingOnDelete() throws Exception {
        doThrow(new ResourceNotFoundException("Order not found with id 99"))
                .when(orderService).delete(99L);

        mockMvc.perform(delete("/api/v1/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void _11_ShouldReturn200_WhenStatusUpdateSucceeds() throws Exception {
        OrderResponse confirmed = new OrderResponse(1L, sampleCustomer(), null, 2,
                "ORD-TEST-0001", "CONFIRMED", new BigDecimal("100.00"), List.of());
        when(orderService.updateStatus(eq(1L), eq(OrderStatus.CONFIRMED))).thenReturn(confirmed);

        mockMvc.perform(patch("/api/v1/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderStatusUpdateRequest(OrderStatus.CONFIRMED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void _12_ShouldReturn422_WhenStatusTransitionIsInvalid() throws Exception {
        when(orderService.updateStatus(eq(1L), eq(OrderStatus.PENDING)))
                .thenThrow(new BusinessRuleException("Transition from CONFIRMED to PENDING is not allowed"));

        mockMvc.perform(patch("/api/v1/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderStatusUpdateRequest(OrderStatus.PENDING))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }
}
