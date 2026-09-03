package com.edgareldy.springboottutorial.dto.category;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import java.time.Instant;
import java.util.List;

/**
 * Representation of a {@link com.edgareldy.springboottutorial.entity.Category}
 * returned by the API, never the JPA entity itself. The JSON contract is
 * snake_case, decoupled from the Java fields' own camelCase names via
 * explicit {@link JsonProperty} annotations. {@code products} is only
 * populated on the single-category detail endpoint
 * ({@code GET /api/v1/categories/{id}}): the paginated list endpoint leaves
 * it as an empty list rather than eagerly loading every returned category's
 * full product collection on each page.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public record CategoryResponse(
        Long id,

        @JsonProperty("category_name")
        String categoryName,

        @JsonProperty("created_at")
        Instant createdAt,

        @JsonProperty("updated_at")
        Instant updatedAt,

        List<ProductResponse> products
) {
}
