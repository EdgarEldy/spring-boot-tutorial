package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.order.CustomerOrdersResponse;
import com.edgareldy.springboottutorial.dto.order.OrderRequest;
import com.edgareldy.springboottutorial.dto.order.OrderResponse;
import com.edgareldy.springboottutorial.entity.OrderStatus;
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

    /**
     * Returns the caller's own orders when {@code isAdmin} is false, or all
     * orders in the system when {@code isAdmin} is true. Never throws when the
     * user has no orders; returns an empty page instead.
     */
    PageResponse<OrderResponse> findAll(String username, boolean isAdmin, Pageable pageable);

    /**
     * Returns one entry per customer having at least one matching order,
     * with every product they ordered, its quantity and total, and the sum
     * of those totals. The page counts customers, not orders.
     */
    PageResponse<CustomerOrdersResponse> findAllGroupedByCustomer(Long customerId, Long productId, Pageable pageable);

    /**
     * Returns the order when {@code isAdmin} is true, or only when the order
     * belongs to the caller when {@code isAdmin} is false. Returns 404 in both
     * the missing and the unauthorized cases so ownership is not revealed.
     */
    OrderResponse findById(Long id, String username, boolean isAdmin);

    /**
     * Updates the status of an order. Only an ADMIN may call this method.
     * Allowed transitions: PENDING to CONFIRMED, PENDING to CANCELLED,
     * CONFIRMED to CANCELLED. Any other transition raises
     * {@link com.edgareldy.springboottutorial.exception.BusinessRuleException}.
     */
    OrderResponse updateStatus(Long id, OrderStatus newStatus);

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
