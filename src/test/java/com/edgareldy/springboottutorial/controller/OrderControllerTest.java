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
import com.edgareldy.springboottutorial.dto.order.CustomerOrdersResponse;
import com.edgareldy.springboottutorial.dto.order.OrderCustomerResponse;
import com.edgareldy.springboottutorial.dto.order.OrderRequest;
import com.edgareldy.springboottutorial.dto.order.OrderResponse;
import com.edgareldy.springboottutorial.dto.order.OrderedProductResponse;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.service.OrderService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc integration tests for {@link OrderController}, with
 * {@link OrderService} mocked. Security filters are disabled since
 * {@code feature/auth} has not defined a {@code SecurityConfig} yet.
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

    private static OrderCustomerResponse sampleCustomer() {
        return new OrderCustomerResponse(
                1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way");
    }

    private static OrderResponse sampleResponse() {
        ProductResponse product = new ProductResponse(1L, "Keyboard", 50.0f, 1L, "Electronics");
        return new OrderResponse(1L, sampleCustomer(), product, 2, 100.0);
    }

    private static PageResponse<CustomerOrdersResponse> sampleGroupedPage() {
        CustomerOrdersResponse entry = new CustomerOrdersResponse(
                sampleCustomer(),
                List.of(new OrderedProductResponse(1L, 1L, "Keyboard", 50.0f, 2, 100.0),
                        new OrderedProductResponse(2L, 2L, "Desk", 200.0f, 1, 200.0)),
                300.0);
        return new PageResponse<>(List.of(entry), 0, 20, 1, 1);
    }

    @Test
    void _01_ShouldReturnOrdersGroupedByCustomer_WhenNoFilterGiven() throws Exception {
        when(orderService.findAllGroupedByCustomer(isNull(), isNull(), any())).thenReturn(sampleGroupedPage());

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].customer.first_name").value("Ada"))
                .andExpect(jsonPath("$.data.content[0].customer.products").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].products.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].products[0].order_id").value(1))
                .andExpect(jsonPath("$.data.content[0].products[0].product_name").value("Keyboard"))
                .andExpect(jsonPath("$.data.content[0].products[0].unit_price").value(50.0))
                .andExpect(jsonPath("$.data.content[0].products[0].quantity").value(2))
                .andExpect(jsonPath("$.data.content[0].products[0].total").value(100.0))
                .andExpect(jsonPath("$.data.content[0].products[1].product_id").value(2))
                .andExpect(jsonPath("$.data.content[0].total").value(300.0));
    }

    @Test
    void _02_ShouldForwardFilters_WhenCustomerAndProductGiven() throws Exception {
        when(orderService.findAllGroupedByCustomer(eq(1L), eq(1L), any())).thenReturn(sampleGroupedPage());

        mockMvc.perform(get("/api/v1/orders").param("customerId", "1").param("productId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].customer.first_name").value("Ada"));
    }

    @Test
    void _03_ShouldReturn404_WhenOrderMissing() throws Exception {
        when(orderService.findById(99L)).thenThrow(new ResourceNotFoundException("Order not found with id 99"));

        mockMvc.perform(get("/api/v1/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _04_ShouldReturn201_WhenCreateRequestValid() throws Exception {
        when(orderService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(1L, 1L, 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.product.product_name").value("Keyboard"));
    }

    @Test
    void _05_ShouldReturn400_WhenQuantityNotPositive() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(1L, 1L, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _06_ShouldReturn404_WhenCustomerMissingOnCreate() throws Exception {
        when(orderService.create(any())).thenThrow(new ResourceNotFoundException("Customer not found with id 99"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(99L, 1L, 2))))
                .andExpect(status().isNotFound());
    }

    @Test
    void _07_ShouldReturn200_WhenUpdateRequestValid() throws Exception {
        when(orderService.update(eq(1L), any())).thenReturn(sampleResponse());

        mockMvc.perform(put("/api/v1/orders/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(1L, 1L, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(2));
    }

    @Test
    void _08_ShouldReturn404_WhenOrderMissingOnUpdate() throws Exception {
        when(orderService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Order not found with id 99"));

        mockMvc.perform(put("/api/v1/orders/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderRequest(1L, 1L, 2))))
                .andExpect(status().isNotFound());
    }

    @Test
    void _09_ShouldReturn200_WhenDeleteSucceeds() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void _10_ShouldReturn404_WhenOrderMissingOnDelete() throws Exception {
        doThrow(new ResourceNotFoundException("Order not found with id 99"))
                .when(orderService).delete(99L);

        mockMvc.perform(delete("/api/v1/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
