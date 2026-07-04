package edgareldy.springboottutorial.mapper;

import edgareldy.springboottutorial.dto.product.ProductRequest;
import edgareldy.springboottutorial.dto.product.ProductResponse;
import edgareldy.springboottutorial.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper converting between {@link Product} and its DTOs. The
 * {@code category} association itself is resolved by the service layer
 * (which needs a repository lookup), not by this mapper: every mapping
 * method that touches an entity ignores the {@code category} field.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.categoryName", target = "categoryName")
    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    Product toEntity(ProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateEntityFromRequest(ProductRequest request, @MappingTarget Product product);
}
