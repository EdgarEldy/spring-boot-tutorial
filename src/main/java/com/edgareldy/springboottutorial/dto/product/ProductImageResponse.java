package com.edgareldy.springboottutorial.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representation of a {@link com.edgareldy.springboottutorial.entity.ProductImage}
 * returned by the API. Fields are exposed in snake_case via explicit
 * {@link JsonProperty} annotations while the Java record fields remain in
 * camelCase, keeping the same convention as the other response DTOs.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
public record ProductImageResponse(
        Long id,

        @JsonProperty("image_url")
        String imageUrl,

        @JsonProperty("alt_text")
        String altText,

        @JsonProperty("is_primary")
        boolean primary,

        @JsonProperty("display_order")
        int displayOrder
) {
}
