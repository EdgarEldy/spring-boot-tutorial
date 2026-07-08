package edgareldy.springboottutorial.mapper;

import edgareldy.springboottutorial.dto.order.OrderRequest;
import edgareldy.springboottutorial.dto.order.OrderResponse;
import edgareldy.springboottutorial.entity.Customer;
import edgareldy.springboottutorial.entity.Order;
import edgareldy.springboottutorial.entity.Product;
import edgareldy.springboottutorial.repository.OrderProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting between {@link Order} and its DTOs. Two
 * {@code toResponse} overloads exist because the service builds an
 * {@link OrderResponse} from two different sources: a full {@link Order}
 * entity (single-item detail, via {@code findByIdWithDetails}) or a flat
 * {@link OrderProjection} row (paginated lists, via {@code findAllProjected}).
 * The {@code customer}/{@code product} associations are resolved by the
 * service layer, not by this mapper, since that requires repository
 * lookups.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    default OrderResponse toResponse(OrderProjection projection) {
        return new OrderResponse(
                projection.id(),
                new OrderResponse.CustomerSummary(projection.customerId(), projection.customerFullName()),
                new OrderResponse.ProductSummary(projection.productId(), projection.productName(), projection.productUnitPrice()),
                projection.quantity(),
                projection.total()
        );
    }

    default OrderResponse.CustomerSummary toCustomerSummary(Customer customer) {
        return new OrderResponse.CustomerSummary(customer.getId(), customer.getFirstName() + " " + customer.getLastName());
    }

    default OrderResponse.ProductSummary toProductSummary(Product product) {
        return new OrderResponse.ProductSummary(product.getId(), product.getProductName(), product.getUnitPrice());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "total", ignore = true)
    Order toEntity(OrderRequest request);
}
