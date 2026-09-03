package edgareldy.springboottutorial.dto.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload accepted by {@code POST}/{@code PUT} {@code /api/v1/products}.
 * {@code categoryId} must reference an existing category; the service
 * layer, not this record, is responsible for checking that. The JSON
 * contract is snake_case, decoupled from the Java fields' own camelCase
 * names via explicit {@link JsonProperty} annotations.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public record ProductRequest(

        @JsonProperty("category_id")
        @NotNull(message = "Category id must not be null")
        Long categoryId,

        @JsonProperty("product_name")
        @NotBlank(message = "Product name must not be blank")
        String productName,

        @JsonProperty("unit_price")
        @NotNull(message = "Unit price must not be null")
        @Positive(message = "Unit price must be greater than 0")
        Float unitPrice
) {
}
