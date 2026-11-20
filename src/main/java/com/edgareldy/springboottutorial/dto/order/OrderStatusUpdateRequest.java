package com.edgareldy.springboottutorial.dto.order;

import com.edgareldy.springboottutorial.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for the PATCH /api/v1/orders/{id}/status endpoint.
 * Carries the target {@link OrderStatus}; only an ADMIN may call this
 * endpoint. Allowed transitions: PENDING to CONFIRMED, PENDING to CANCELLED,
 * CONFIRMED to CANCELLED.
 * <p>
 * Created edgar.muhamyangabo on 11/20/26
 * Author : edgar.muhamyangabo
 * Date : 11/20/26
 * Project : spring-boot-tutorial
 */
public record OrderStatusUpdateRequest(
        @JsonProperty("status")
        @NotNull(message = "Status must not be null")
        OrderStatus status
) {}
