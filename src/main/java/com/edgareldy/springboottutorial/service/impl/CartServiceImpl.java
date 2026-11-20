package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.dto.cart.CartItemRequest;
import com.edgareldy.springboottutorial.dto.cart.CartItemUpdateRequest;
import com.edgareldy.springboottutorial.dto.cart.CartResponse;
import com.edgareldy.springboottutorial.entity.Cart;
import com.edgareldy.springboottutorial.entity.CartItem;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.mapper.CartMapper;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.repository.CartItemRepository;
import com.edgareldy.springboottutorial.repository.CartRepository;
import com.edgareldy.springboottutorial.repository.ProductRepository;
import com.edgareldy.springboottutorial.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link CartService}. Each user has at most one
 * cart, created lazily on the first item add. The user is always identified
 * by username (from the SecurityContext), never by a userId supplied in the
 * HTTP payload.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository appUserRepository;
    private final CartMapper cartMapper;

    @Override
    public CartResponse getCart(String username) {
        AppUser user = getUserOrThrow(username);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> Cart.builder().user(user).build());
        return cartMapper.toResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(String username, CartItemRequest request) {
        AppUser user = getUserOrThrow(username);
        Product product = getActiveProductOrThrow(request.productId());
        Cart cart = getOrCreateCart(user);

        cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .ifPresentOrElse(
                        existing -> existing.setQuantity(existing.getQuantity() + request.quantity()),
                        () -> {
                            CartItem item = CartItem.builder()
                                    .cart(cart)
                                    .product(product)
                                    .quantity(request.quantity())
                                    .build();
                            cart.getItems().add(item);
                        }
                );
        Cart saved = cartRepository.save(cart);
        Cart loaded = cartRepository.findByUserId(user.getId()).orElse(saved);
        return cartMapper.toResponse(loaded);
    }

    @Override
    @Transactional
    public CartResponse updateItem(String username, Long itemId, CartItemUpdateRequest request) {
        AppUser user = getUserOrThrow(username);
        Cart cart = getCartOrThrow(user);
        CartItem item = getItemBelongingToCart(cart, itemId);
        item.setQuantity(request.quantity());
        cartRepository.save(cart);
        Cart loaded = cartRepository.findByUserId(user.getId()).orElse(cart);
        return cartMapper.toResponse(loaded);
    }

    @Override
    @Transactional
    public CartResponse removeItem(String username, Long itemId) {
        AppUser user = getUserOrThrow(username);
        Cart cart = getCartOrThrow(user);
        CartItem item = getItemBelongingToCart(cart, itemId);
        cart.getItems().remove(item);
        cartRepository.save(cart);
        Cart loaded = cartRepository.findByUserId(user.getId()).orElse(cart);
        return cartMapper.toResponse(loaded);
    }

    @Override
    @Transactional
    public CartResponse clearCart(String username) {
        AppUser user = getUserOrThrow(username);
        Cart cart = getCartOrThrow(user);
        cart.getItems().clear();
        cartRepository.save(cart);
        Cart loaded = cartRepository.findByUserId(user.getId()).orElse(cart);
        return cartMapper.toResponse(loaded);
    }

    private AppUser getUserOrThrow(String username) {
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + username));
    }

    private Product getActiveProductOrThrow(Long productId) {
        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id " + productId));
        if (!product.isActive()) {
            throw new BusinessRuleException(
                    "Product with id " + productId + " is not active and cannot be added to cart");
        }
        return product;
    }

    private Cart getOrCreateCart(AppUser user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(user).build()));
    }

    private Cart getCartOrThrow(AppUser user) {
        return cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user: " + user.getUsername()));
    }

    private CartItem getItemBelongingToCart(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id " + itemId));
    }
}
