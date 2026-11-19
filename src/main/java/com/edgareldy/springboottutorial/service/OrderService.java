package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.order.CustomerOrdersResponse;
import com.edgareldy.springboottutorial.dto.order.OrderRequest;
import com.edgareldy.springboottutorial.dto.order.OrderResponse;
import org.springframework.data.domain.Pageable;

/**
 * Contract for {@link com.edgareldy.springboottutorial.entity.Order} business
 * operations. Controllers and tests depend on this interface, never on its
 * implementation directly.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public interface OrderService {

    PageResponse<OrderResponse> findAll(Long customerId, Long productId, Pageable pageable);

    /**
     * Returns one entry per customer having at least one matching order,
     * with every product they ordered, its quantity and total, and the sum
     * of those totals. The page counts customers, not orders.
     */
    PageResponse<CustomerOrdersResponse> findAllGroupedByCustomer(Long customerId, Long productId, Pageable pageable);

    OrderResponse findById(Long id);

    /**
     * Converts the authenticated user's cart into a new Order atomically.
     * The cart is cleared only if the Order is persisted successfully;
     * a rollback leaves the cart untouched. Prices are always re-read from
     * the database, never trusted from the cart items.
     */
    OrderResponse checkout(String username);

    OrderResponse create(OrderRequest request);

    OrderResponse update(Long id, OrderRequest request);

    void delete(Long id);
}
