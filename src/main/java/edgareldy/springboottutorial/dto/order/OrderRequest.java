package edgareldy.springboottutorial.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Payload accepted by {@code POST}/{@code PUT} {@code /api/v1/orders}.
 * {@code total} is never part of the request: the service always
 * computes it as {@code quantity * product.unitPrice}.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public record OrderRequest(

        @NotNull(message = "customerId must not be null")
        Long customerId,

        @NotNull(message = "productId must not be null")
        Long productId,

        @NotNull(message = "quantity must not be null")
        @Positive(message = "quantity must be greater than 0")
        Integer quantity
) {
}
