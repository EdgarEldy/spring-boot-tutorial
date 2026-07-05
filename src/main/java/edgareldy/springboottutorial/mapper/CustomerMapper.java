package edgareldy.springboottutorial.mapper;

import edgareldy.springboottutorial.dto.customer.CustomerRequest;
import edgareldy.springboottutorial.dto.customer.CustomerResponse;
import edgareldy.springboottutorial.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper converting between {@link Customer} and its DTOs.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "id", ignore = true)
    Customer toEntity(CustomerRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(CustomerRequest request, @MappingTarget Customer customer);
}
