package edgareldy.springboottutorial.mapper;

import edgareldy.springboottutorial.dto.customer.CustomerRequest;
import edgareldy.springboottutorial.dto.customer.CustomerResponse;
import edgareldy.springboottutorial.dto.product.ProductResponse;
import edgareldy.springboottutorial.entity.Customer;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper converting between {@link Customer} and its DTOs. Two
 * response methods exist for the same reason {@code CategoryMapper} has
 * {@code toResponse}/{@code toDetailResponse}: {@link #toResponse(Customer)}
 * always leaves {@code products} as an empty list (used by the paginated
 * list, and when nesting a customer inside an {@code OrderResponse}),
 * {@link #toDetailResponse(Customer, List)} takes the caller-supplied list
 * of ordered products (resolved by the service layer via
 * {@code OrderRepository}, since {@link Customer} itself has no direct
 * association to {@code Order}/{@code Product}) for the single-customer
 * detail endpoint.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "products", expression = "java(java.util.List.of())")
    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "products", source = "products")
    CustomerResponse toDetailResponse(Customer customer, List<ProductResponse> products);

    @Mapping(target = "id", ignore = true)
    Customer toEntity(CustomerRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(CustomerRequest request, @MappingTarget Customer customer);
}
