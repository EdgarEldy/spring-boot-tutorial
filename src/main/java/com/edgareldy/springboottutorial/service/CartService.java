package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.dto.cart.CartItemRequest;
import com.edgareldy.springboottutorial.dto.cart.CartItemUpdateRequest;
import com.edgareldy.springboottutorial.dto.cart.CartResponse;

/**
 * Contract for shopping-cart operations. All methods receive the username
 * of the authenticated user (resolved from the SecurityContext by the
 * controller), never a userId from the HTTP payload.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
public interface CartService {

    CartResponse getCart(String username);

    CartResponse addItem(String username, CartItemRequest request);

    CartResponse updateItem(String username, Long itemId, CartItemUpdateRequest request);

    CartResponse removeItem(String username, Long itemId);

    CartResponse clearCart(String username);
}
