package com.edgareldy.springboottutorial.mapper;

import com.edgareldy.springboottutorial.dto.customer.CustomerResponse;
import com.edgareldy.springboottutorial.dto.order.OrderRequest;
import com.edgareldy.springboottutorial.dto.order.OrderResponse;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.repository.OrderProjection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting between {@link Order} and its DTOs. Two
 * {@code toResponse} overloads exist because the service builds an
 * {@link OrderResponse} from two different sources: a full {@link Order}
 * entity (single-item detail, via {@code findByIdWithDetails}, where
 * {@code customer}/{@code product} are mapped automatically through
 * {@code uses}) or a flat {@link OrderProjection} row (paginated lists,
 * via {@code findAllProjected}, built manually since there is no
 * {@code Customer}/{@code Product} entity to delegate to).
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring", uses = {CustomerMapper.class, ProductMapper.class})
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    default OrderResponse toResponse(OrderProjection projection) {
        CustomerResponse customer = new CustomerResponse(
                projection.customerId(),
                projection.customerFirstName(),
                projection.customerLastName(),
                projection.customerTelephone(),
                projection.customerEmail(),
                projection.customerAddress(),
                List.of());
        ProductResponse product = new ProductResponse(
                projection.productId(),
                projection.productName(),
                projection.productUnitPrice(),
                projection.productCategoryId(),
                projection.productCategoryName());
        return new OrderResponse(projection.id(), customer, product, projection.quantity(), projection.total());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "total", ignore = true)
    Order toEntity(OrderRequest request);
}
