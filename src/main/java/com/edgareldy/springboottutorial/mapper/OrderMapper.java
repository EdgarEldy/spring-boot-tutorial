package com.edgareldy.springboottutorial.mapper;

import com.edgareldy.springboottutorial.dto.order.CustomerOrdersResponse;
import com.edgareldy.springboottutorial.dto.order.OrderCustomerResponse;
import com.edgareldy.springboottutorial.dto.order.OrderRequest;
import com.edgareldy.springboottutorial.dto.order.OrderResponse;
import com.edgareldy.springboottutorial.dto.order.OrderedProductResponse;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import com.edgareldy.springboottutorial.entity.Customer;
import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.repository.OrderProjection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper converting between {@link Order} and its DTOs. Two
 * {@code toResponse} overloads exist because the service builds an
 * {@link OrderResponse} from two different sources: a full {@link Order}
 * entity (single-item detail, via {@code findByIdWithDetails}, where
 * {@code customer} goes through {@link #toCustomerResponse(Customer)} and
 * {@code product} through {@code ProductMapper}) or a flat
 * {@link OrderProjection} row (paginated lists, via
 * {@code findAllProjected}, built manually since there is no
 * {@code Customer}/{@code Product} entity to delegate to).
 * {@link #toCustomerOrdersResponse} builds the grouped view of
 * {@code GET /api/v1/orders}.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring", uses = ProductMapper.class)
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    OrderCustomerResponse toCustomerResponse(Customer customer);

    /**
     * Assembles one grouped entry from a customer and their order rows. The
     * {@code total} is computed by the service, the mapper only assigns it.
     */
    default CustomerOrdersResponse toCustomerOrdersResponse(
            Customer customer, List<OrderProjection> orders, double total) {
        return new CustomerOrdersResponse(toCustomerResponse(customer), toOrderedProducts(orders), total);
    }

    /**
     * Turns order rows into "ordered product" lines, one per order. Shared by
     * the grouped order view and by the customer responses.
     */
    default List<OrderedProductResponse> toOrderedProducts(List<OrderProjection> orders) {
        return orders.stream()
                .map(order -> new OrderedProductResponse(
                        order.id(),
                        order.productId(),
                        order.productName(),
                        order.productUnitPrice(),
                        order.quantity(),
                        order.total()))
                .toList();
    }

    default OrderResponse toResponse(OrderProjection projection) {
        OrderCustomerResponse customer = new OrderCustomerResponse(
                projection.customerId(),
                projection.customerFirstName(),
                projection.customerLastName(),
                projection.customerTelephone(),
                projection.customerEmail(),
                projection.customerAddress());
        ProductResponse product = new ProductResponse(
                projection.productId(),
                projection.productName(),
                projection.productUnitPrice(),
                projection.productCategoryId(),
                projection.productCategoryName());
        return new OrderResponse(projection.id(), customer, product, projection.quantity(), projection.total());
    }

    /**
     * Builds a new {@link Order} from the request plus the already resolved
     * {@code customer} and {@code product}, and the {@code total} the service
     * computed. The mapper only assigns these values: looking the associations
     * up and computing the total are business decisions that stay in the
     * service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", source = "customer")
    @Mapping(target = "product", source = "product")
    @Mapping(target = "quantity", source = "request.quantity")
    @Mapping(target = "total", source = "total")
    Order toEntity(OrderRequest request, Customer customer, Product product, double total);

    /**
     * Applies the same values onto an existing {@link Order}, keeping its
     * identity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", source = "customer")
    @Mapping(target = "product", source = "product")
    @Mapping(target = "quantity", source = "request.quantity")
    @Mapping(target = "total", source = "total")
    void updateEntity(OrderRequest request, Customer customer, Product product, double total,
                      @MappingTarget Order order);
}
