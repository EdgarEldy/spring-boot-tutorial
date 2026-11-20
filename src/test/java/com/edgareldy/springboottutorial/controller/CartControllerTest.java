package com.edgareldy.springboottutorial.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgareldy.springboottutorial.dto.cart.CartItemRequest;
import com.edgareldy.springboottutorial.dto.cart.CartItemUpdateRequest;
import com.edgareldy.springboottutorial.dto.cart.CartItemResponse;
import com.edgareldy.springboottutorial.dto.cart.CartResponse;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.security.JwtService;
import com.edgareldy.springboottutorial.service.BlacklistedTokenService;
import com.edgareldy.springboottutorial.service.CartService;
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
 * MockMvc integration tests for {@link CartController}, with
 * {@link CartService} mocked. Security filters are disabled; the
 * authenticated user is injected via {@code @WithMockUser} at class
 * level, which populates {@code SecurityContextHolder} directly (not
 * through the filter chain) so that {@code @AuthenticationPrincipal}
 * resolves "alice" in every test.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(username = "alice", roles = "USER")
class CartControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CartService cartService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private BlacklistedTokenService blacklistedTokenService;

    private static CartResponse emptyCart() {
        return new CartResponse(1L, List.of(), BigDecimal.ZERO);
    }

    private static CartResponse cartWithItem() {
        CartItemResponse item = new CartItemResponse(
                1L, 1L, "Keyboard", new BigDecimal("79.99"), 2, new BigDecimal("159.98"));
        return new CartResponse(1L, List.of(item), new BigDecimal("159.98"));
    }

    @Test
    void _01_ShouldReturn200_WhenGetCart() throws Exception {
        when(cartService.getCart("alice")).thenReturn(emptyCart());

        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void _02_ShouldReturn201_WhenAddItemValid() throws Exception {
        when(cartService.addItem(eq("alice"), any())).thenReturn(cartWithItem());

        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemRequest(1L, 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.items[0].product_name").value("Keyboard"))
                .andExpect(jsonPath("$.data.items[0].unit_price").value(79.99))
                .andExpect(jsonPath("$.data.total").value(159.98));
    }

    @Test
    void _03_ShouldReturn400_WhenAddItemQuantityZero() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemRequest(1L, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _04_ShouldReturn200_WhenUpdateItemValid() throws Exception {
        when(cartService.updateItem(eq("alice"), eq(1L), any())).thenReturn(cartWithItem());

        mockMvc.perform(patch("/api/v1/cart/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemUpdateRequest(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
    }

    @Test
    void _05_ShouldReturn404_WhenUpdateItemNotFound() throws Exception {
        when(cartService.updateItem(eq("alice"), eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Cart item not found with id 99"));

        mockMvc.perform(patch("/api/v1/cart/items/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemUpdateRequest(1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void _06_ShouldReturn200_WhenRemoveItem() throws Exception {
        when(cartService.removeItem("alice", 1L)).thenReturn(emptyCart());

        mockMvc.perform(delete("/api/v1/cart/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void _07_ShouldReturn200_WhenClearCart() throws Exception {
        when(cartService.clearCart("alice")).thenReturn(emptyCart());

        mockMvc.perform(delete("/api/v1/cart/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());
    }
}
