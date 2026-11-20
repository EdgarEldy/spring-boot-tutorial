package com.edgareldy.springboottutorial.mapper;

import com.edgareldy.springboottutorial.dto.cart.CartItemResponse;
import com.edgareldy.springboottutorial.dto.cart.CartResponse;
import com.edgareldy.springboottutorial.entity.Cart;
import com.edgareldy.springboottutorial.entity.CartItem;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting {@link Cart} and {@link CartItem} entities to
 * their response DTOs. The {@code subtotal} per item and the cart {@code total}
 * are computed fields: MapStruct cannot derive them automatically, so
 * {@code toItemResponse} uses an inline expression and {@code toResponse} is
 * a {@code default} method that sums the mapped item subtotals.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "unitPrice", source = "product.unitPrice")
    @Mapping(target = "subtotal",
            expression = "java(item.getProduct().getUnitPrice()"
                    + ".multiply(java.math.BigDecimal.valueOf(item.getQuantity())))")
    CartItemResponse toItemResponse(CartItem item);

    List<CartItemResponse> toItemResponseList(List<CartItem> items);

    default CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = toItemResponseList(cart.getItems());
        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, total);
    }
}
