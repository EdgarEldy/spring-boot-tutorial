package com.edgareldy.springboottutorial.mapper;

import com.edgareldy.springboottutorial.dto.order.CustomerOrdersResponse;
import com.edgareldy.springboottutorial.dto.order.OrderCustomerResponse;
import com.edgareldy.springboottutorial.dto.order.OrderResponse;
import com.edgareldy.springboottutorial.dto.order.OrderedProductResponse;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import com.edgareldy.springboottutorial.entity.Customer;
import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.repository.OrderProjection;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting between {@link Order} and its DTOs. Two
 * {@code toResponse} overloads handle the two read paths: a full {@link Order}
 * entity (single-item detail, via {@code findByIdWithDetails}) and a flat
 * {@link OrderProjection} row (paginated lists, via {@code findAllProjected}).
 * {@link OrderItemMapper} is included via {@code uses} so MapStruct can
 * auto-map {@code Order.items} to {@code List<OrderItemResponse>}.
 * {@link #toCustomerOrdersResponse} builds the grouped view of
 * {@code GET /api/v1/orders}, and {@link #toOrderedProducts} assembles the
 * per-order lines used in both the grouped view and customer detail responses.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring", uses = {ProductMapper.class, OrderItemMapper.class})
public interface OrderMapper {

    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    OrderResponse toResponse(Order order);

    OrderCustomerResponse toCustomerResponse(Customer customer);

    /**
     * Assembles one grouped entry from a customer and their order rows. The
     * {@code total} is computed by the service, the mapper only assigns it.
     */
    default CustomerOrdersResponse toCustomerOrdersResponse(
            Customer customer, List<OrderProjection> orders, BigDecimal total) {
        return new CustomerOrdersResponse(toCustomerResponse(customer), toOrderedProducts(orders), total);
    }

    /**
     * Turns order projection rows into ordered-product lines. Each row maps
     * to one order placed by the customer. Used in the grouped order view and
     * in customer detail responses.
     */
    default List<OrderedProductResponse> toOrderedProducts(List<OrderProjection> orders) {
        return orders.stream()
                .map(o -> new OrderedProductResponse(
                        o.id(),
                        o.productId(),
                        o.productName(),
                        o.productUnitPrice(),
                        o.quantity(),
                        o.totalAmount()))
                .toList();
    }

    /**
     * Builds an {@link OrderResponse} from a flat projection row (list path).
     * {@code description} is not included in the projection and maps to
     * {@code null}; {@code active} is sourced from {@code p.active} in the
     * JPQL query.
     */
    default OrderResponse toResponse(OrderProjection p) {
        OrderCustomerResponse customer = new OrderCustomerResponse(
                p.customerId(),
                p.customerFirstName(),
                p.customerLastName(),
                p.customerTelephone(),
                p.customerEmail(),
                p.customerAddress());
        ProductResponse product = new ProductResponse(
                p.productId(),
                p.productName(),
                p.productUnitPrice(),
                null,
                p.productActive(),
                p.productCategoryId(),
                p.productCategoryName(),
                List.of());
        return new OrderResponse(
                p.id(), customer, product, p.quantity(),
                p.orderNumber(), p.status().name(), p.totalAmount(), List.of());
    }
}
