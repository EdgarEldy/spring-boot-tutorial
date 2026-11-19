package com.edgareldy.springboottutorial.controller;

import com.edgareldy.springboottutorial.dto.cart.CartItemRequest;
import com.edgareldy.springboottutorial.dto.cart.CartItemUpdateRequest;
import com.edgareldy.springboottutorial.dto.cart.CartResponse;
import com.edgareldy.springboottutorial.dto.common.ApiResponse;
import com.edgareldy.springboottutorial.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the authenticated user's shopping cart.
 * All endpoints require the USER role (enforced by SecurityConfig). The
 * user identity is always taken from the SecurityContext via
 * {@code @AuthenticationPrincipal UserDetails}, never from the payload.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management for authenticated users")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get the authenticated user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success(cartService.getCart(userDetails.getUsername()),
                        "Cart retrieved successfully"));
    }

    @PostMapping("/items")
    @Operation(summary = "Add an item to the cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        cartService.addItem(userDetails.getUsername(), request),
                        "Item added to cart"));
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Update the quantity of a cart item")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId,
            @Valid @RequestBody CartItemUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        cartService.updateItem(userDetails.getUsername(), itemId, request),
                        "Cart item updated"));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove a single item from the cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        cartService.removeItem(userDetails.getUsername(), itemId),
                        "Cart item removed"));
    }

    @DeleteMapping("/items")
    @Operation(summary = "Clear all items from the cart")
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success(cartService.clearCart(userDetails.getUsername()),
                        "Cart cleared"));
    }
}
