package com.edgareldy.springboottutorial.dto.order;

import com.edgareldy.springboottutorial.dto.customer.CustomerResponse;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;

/**
 * Representation of a {@link com.edgareldy.springboottutorial.entity.Order}
 * returned by the API. Carries the full {@link CustomerResponse}/
 * {@link ProductResponse} shapes for {@code customer}/{@code product}
 * rather than a summarized sub-object, so a caller never has to make a
 * second request just to get the customer's address or the product's
 * category.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public record OrderResponse(
        Long id,
        CustomerResponse customer,
        ProductResponse product,
        int quantity,
        double total
) {
}
