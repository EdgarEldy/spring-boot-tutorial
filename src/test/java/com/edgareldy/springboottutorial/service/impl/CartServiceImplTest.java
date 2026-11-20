package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.dto.cart.CartItemRequest;
import com.edgareldy.springboottutorial.dto.cart.CartItemUpdateRequest;
import com.edgareldy.springboottutorial.dto.cart.CartItemResponse;
import com.edgareldy.springboottutorial.dto.cart.CartResponse;
import com.edgareldy.springboottutorial.entity.Cart;
import com.edgareldy.springboottutorial.entity.CartItem;
import com.edgareldy.springboottutorial.entity.Category;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.mapper.CartMapper;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.repository.CartItemRepository;
import com.edgareldy.springboottutorial.repository.CartRepository;
import com.edgareldy.springboottutorial.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CartServiceImpl}, with all collaborators mocked.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private CartMapper cartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private AppUser user;
    private Product product;
    private Cart cart;
    private CartItem cartItem;
    private CartResponse emptyResponse;
    private CartResponse filledResponse;

    @BeforeEach
    void setUp() {
        Category category = Category.builder().id(1L).categoryName("Electronics").build();
        user = AppUser.builder().id(1L).username("alice").email("alice@example.com").build();
        product = Product.builder()
                .id(1L).productName("Keyboard").unitPrice(new BigDecimal("79.99"))
                .category(category).active(true).build();
        cart = Cart.builder().id(1L).user(user).items(new ArrayList<>()).build();
        cartItem = CartItem.builder().id(1L).cart(cart).product(product).quantity(2).build();
        emptyResponse = new CartResponse(1L, List.of(), BigDecimal.ZERO);
        filledResponse = new CartResponse(1L,
                List.of(new CartItemResponse(1L, 1L, "Keyboard",
                        new BigDecimal("79.99"), 2, new BigDecimal("159.98"))),
                new BigDecimal("159.98"));
    }

    @Test
    void getCart_ShouldReturnExistingCart() {
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(emptyResponse);

        CartResponse result = cartService.getCart("alice");

        assertThat(result).isEqualTo(emptyResponse);
    }

    @Test
    void getCart_ShouldReturnEmptyCart_WhenNoneExists() {
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());

        CartResponse result = cartService.getCart("alice");

        // in-memory empty cart is returned, mapper called with a transient Cart
        verify(cartMapper).toResponse(any(Cart.class));
        assertThat(result).isNull(); // mapper is mocked, returns null by default
    }

    @Test
    void addItem_ShouldCreateNewCartAndAddItem_WhenNoCartExists() {
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
        // getOrCreateCart: first call returns empty, so save is called
        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartItemRepository.findByCartIdAndProductId(any(), eq(1L))).thenReturn(Optional.empty());
        when(cartMapper.toResponse(cart)).thenReturn(filledResponse);

        CartResponse result = cartService.addItem("alice", new CartItemRequest(1L, 2));

        assertThat(result).isEqualTo(filledResponse);
        // save is called twice: once to create the new empty cart, once to persist the new item
        verify(cartRepository, org.mockito.Mockito.times(2)).save(any(Cart.class));
    }

    @Test
    void addItem_ShouldIncrementQuantity_WhenProductAlreadyInCart() {
        cart.getItems().add(cartItem);
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(cartItem));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(filledResponse);

        cartService.addItem("alice", new CartItemRequest(1L, 3));

        assertThat(cartItem.getQuantity()).isEqualTo(5); // 2 + 3
    }

    @Test
    void addItem_ShouldThrowBusinessRuleException_WhenProductInactive() {
        product.setActive(false);
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addItem("alice", new CartItemRequest(1L, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void addItem_ShouldThrowResourceNotFoundException_WhenProductNotFound() {
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(productRepository.findByIdWithCategory(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem("alice", new CartItemRequest(99L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItem_ShouldChangeQuantity() {
        cart.getItems().add(cartItem);
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart))
                .thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(filledResponse);

        cartService.updateItem("alice", 1L, new CartItemUpdateRequest(5));

        assertThat(cartItem.getQuantity()).isEqualTo(5);
    }

    @Test
    void updateItem_ShouldThrowResourceNotFoundException_WhenItemNotInCart() {
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateItem("alice", 99L, new CartItemUpdateRequest(1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Cart item not found");
    }

    @Test
    void removeItem_ShouldRemoveItemFromCart() {
        cart.getItems().add(cartItem);
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart))
                .thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(emptyResponse);

        cartService.removeItem("alice", 1L);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void clearCart_ShouldRemoveAllItems() {
        cart.getItems().add(cartItem);
        when(appUserRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(cart))
                .thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(emptyResponse);

        CartResponse result = cartService.clearCart("alice");

        assertThat(cart.getItems()).isEmpty();
        assertThat(result).isEqualTo(emptyResponse);
    }
}
