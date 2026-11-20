package com.edgareldy.springboottutorial.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

/**
 * Representation of a {@link com.edgareldy.springboottutorial.entity.Product}
 * returned by the API. Carries the parent category as a flat
 * {@code categoryId}/{@code categoryName} pair instead of a nested
 * {@code CategoryResponse}, matching the response shape documented in the
 * README. The JSON contract is snake_case, decoupled from the Java fields'
 * own camelCase names via explicit {@link JsonProperty} annotations.
 * {@code unitPrice} uses {@link BigDecimal} to match the NUMERIC(12,2) column.
 * {@code active} indicates whether the product is available for purchase.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public record ProductResponse(
        Long id,

        @JsonProperty("product_name")
        String productName,

        @JsonProperty("unit_price")
        BigDecimal unitPrice,

        String description,

        boolean active,

        @JsonProperty("category_id")
        Long categoryId,

        @JsonProperty("category_name")
        String categoryName,

        List<ProductImageResponse> images
) {
}
