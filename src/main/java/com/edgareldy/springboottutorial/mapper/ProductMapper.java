package com.edgareldy.springboottutorial.mapper;

import com.edgareldy.springboottutorial.dto.product.ProductImageResponse;
import com.edgareldy.springboottutorial.dto.product.ProductRequest;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import com.edgareldy.springboottutorial.entity.Category;
import com.edgareldy.springboottutorial.entity.Product;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper converting between {@link Product} and its DTOs. The
 * {@code category} association is looked up by the service layer (which needs
 * a repository lookup) and handed to the entity-building methods as a
 * parameter: this mapper only assigns it, so the service never calls a
 * setter itself. {@code active} is ignored on writes so that create and update
 * operations cannot inadvertently overwrite the flag via {@link ProductRequest}.
 * {@code images} is always set to an empty list by {@link #toResponse(Product)};
 * the detail view uses {@link #toDetailResponse(Product, List)} which accepts
 * the caller-provided image list to avoid loading images on the list path.
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
    @Mapping(target = "images", expression = "java(java.util.List.of())")
    ProductResponse toResponse(Product product);

    /**
     * Builds a full detail {@link ProductResponse} with the provided image list.
     * Images are loaded by the service and passed here rather than fetched
     * via a lazy association, keeping this mapper free of repository calls.
     */
    default ProductResponse toDetailResponse(Product product, List<ProductImageResponse> images) {
        ProductResponse base = toResponse(product);
        return new ProductResponse(base.id(), base.productName(), base.unitPrice(),
                base.description(), base.active(), base.categoryId(), base.categoryName(), images);
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    Product toEntity(ProductRequest request, Category category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateEntityFromRequest(ProductRequest request, Category category, @MappingTarget Product product);
}
