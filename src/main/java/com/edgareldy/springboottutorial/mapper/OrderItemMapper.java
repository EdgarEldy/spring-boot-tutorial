package com.edgareldy.springboottutorial.mapper;

import com.edgareldy.springboottutorial.dto.order.OrderItemResponse;
import com.edgareldy.springboottutorial.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting {@link OrderItem} entities to
 * {@link OrderItemResponse} DTOs. The {@code productId} is sourced from the
 * nullable {@code product.id} association; when the product has been deleted
 * (ON DELETE SET NULL), it maps to {@code null}.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring")
public interface OrderItemMapper {

    @Mapping(target = "productId", source = "product.id")
    OrderItemResponse toResponse(OrderItem orderItem);
}
