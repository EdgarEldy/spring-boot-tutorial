package edgareldy.springboottutorial.dto.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload accepted by {@code POST}/{@code PUT} {@code /api/v1/orders}.
 * {@code total} is never part of the request: the service always
 * computes it as {@code quantity * product.unitPrice}. The JSON contract
 * is snake_case, decoupled from the Java fields' own camelCase names via
 * explicit {@link JsonProperty} annotations.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public record OrderRequest(

        @JsonProperty("customer_id")
        @NotNull(message = "Customer id must not be null")
        Long customerId,

        @JsonProperty("product_id")
        @NotNull(message = "Product id must not be null")
        Long productId,

        @NotNull(message = "Quantity must not be null")
        @Positive(message = "Quantity must be greater than 0")
        Integer quantity
) {
}
