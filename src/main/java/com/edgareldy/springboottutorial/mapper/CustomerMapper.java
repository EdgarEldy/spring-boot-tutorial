package com.edgareldy.springboottutorial.mapper;

import com.edgareldy.springboottutorial.dto.customer.CustomerRequest;
import com.edgareldy.springboottutorial.dto.customer.CustomerResponse;
import com.edgareldy.springboottutorial.dto.order.OrderedProductResponse;
import com.edgareldy.springboottutorial.entity.Customer;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper converting between {@link Customer} and its DTOs. Two
 * response methods exist: {@link #toResponse(Customer)} for a customer that
 * cannot have orders yet (just created), with an empty {@code products}
 * list and a total of 0, and
 * {@link #toResponseWithOrders(Customer, List, double)} which takes the
 * ordered products and their total from the caller. They are resolved by
 * the service layer via {@code OrderRepository}, since {@link Customer}
 * itself has no direct association to {@code Order}/{@code Product}.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "products", expression = "java(java.util.List.of())")
    @Mapping(target = "total", constant = "0.0")
    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "products", source = "products")
    @Mapping(target = "total", source = "total")
    CustomerResponse toResponseWithOrders(Customer customer, List<OrderedProductResponse> products, double total);

    @Mapping(target = "id", ignore = true)
    Customer toEntity(CustomerRequest request);

    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(CustomerRequest request, @MappingTarget Customer customer);
}
