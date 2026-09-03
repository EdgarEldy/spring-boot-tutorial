package com.edgareldy.springboottutorial.repository;

/**
 * Flat DTO projection selected directly by a JPQL constructor expression in
 * {@link OrderRepository#findAllProjected}, so listing orders never loads
 * full {@code Order}/{@code Customer}/{@code Product} entities just to
 * discard most of their fields. Distinct from loading a full {@code Order}
 * (used for the single-item detail view), this technique lets the database
 * return exactly the columns the list endpoint needs, carrying every field
 * required to build the full {@code CustomerResponse}/{@code ProductResponse}
 * nested inside an {@code OrderResponse}.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public record OrderProjection(
        Long id,
        Long customerId,
        String customerFirstName,
        String customerLastName,
        String customerTelephone,
        String customerEmail,
        String customerAddress,
        Long productId,
        String productName,
        float productUnitPrice,
        Long productCategoryId,
        String productCategoryName,
        int quantity,
        double total
) {
}
