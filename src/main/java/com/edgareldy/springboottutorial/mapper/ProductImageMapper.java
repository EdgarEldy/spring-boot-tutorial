package com.edgareldy.springboottutorial.mapper;

import com.edgareldy.springboottutorial.dto.product.ProductImageResponse;
import com.edgareldy.springboottutorial.entity.ProductImage;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper converting {@link ProductImage} entities to
 * {@link ProductImageResponse} DTOs. Used by {@link ProductImageServiceImpl}
 * when returning image metadata and by {@link ProductServiceImpl#findById}
 * when building the full product detail response.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
@Mapper(componentModel = "spring")
public interface ProductImageMapper {

    ProductImageResponse toResponse(ProductImage productImage);
}
