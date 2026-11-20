package com.edgareldy.springboottutorial.repository;

import com.edgareldy.springboottutorial.entity.Customer;
import com.edgareldy.springboottutorial.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for {@link Order}.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT new com.edgareldy.springboottutorial.repository.OrderProjection("
            + "o.id, "
            + "c.id, c.firstName, c.lastName, c.telephone, c.email, c.address, "
            + "p.id, p.productName, p.unitPrice, p.active, cat.id, cat.categoryName, "
            + "o.quantity, o.totalAmount, o.status, o.orderNumber) "
            + "FROM Order o JOIN o.customer c JOIN o.product p JOIN p.category cat "
            + "WHERE (:customerId IS NULL OR c.id = :customerId) "
            + "AND (:productId IS NULL OR p.id = :productId)")
    Page<OrderProjection> findAllProjected(@Param("customerId") Long customerId,
                                            @Param("productId") Long productId,
                                            Pageable pageable);

    /**
     * Pages over the customers that have at least one order matching the
     * filters. Paginating customers rather than orders is what lets
     * {@code GET /api/v1/orders} return each customer exactly once.
     */
    @Query("SELECT c FROM Customer c WHERE EXISTS ("
            + "SELECT o.id FROM Order o WHERE o.customer = c "
            + "AND (:customerId IS NULL OR c.id = :customerId) "
            + "AND (:productId IS NULL OR o.product.id = :productId))")
    Page<Customer> findCustomersWithOrders(@Param("customerId") Long customerId,
                                           @Param("productId") Long productId,
                                           Pageable pageable);

    /**
     * Loads, in a single query, every order row of the given customers (the
     * ones of the current page), so grouping never costs one query per customer.
     */
    @Query("SELECT new com.edgareldy.springboottutorial.repository.OrderProjection("
            + "o.id, "
            + "c.id, c.firstName, c.lastName, c.telephone, c.email, c.address, "
            + "p.id, p.productName, p.unitPrice, p.active, cat.id, cat.categoryName, "
            + "o.quantity, o.totalAmount, o.status, o.orderNumber) "
            + "FROM Order o JOIN o.customer c JOIN o.product p JOIN p.category cat "
            + "WHERE c.id IN :customerIds "
            + "AND (:productId IS NULL OR p.id = :productId) "
            + "ORDER BY o.id")
    List<OrderProjection> findAllProjectedByCustomerIds(@Param("customerIds") List<Long> customerIds,
                                                        @Param("productId") Long productId);

    @Query("SELECT o FROM Order o "
            + "JOIN FETCH o.customer "
            + "JOIN FETCH o.product p JOIN FETCH p.category "
            + "LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product "
            + "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    /**
     * Loads the order with its items and owner ({@code AppUser}) for
     * notification purposes. Uses LEFT JOIN FETCH for customer and product
     * so it works for both legacy orders (which have a customer/product FK)
     * and checkout orders (which may not).
     */
    @Query("SELECT o FROM Order o "
            + "LEFT JOIN FETCH o.user "
            + "LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product "
            + "WHERE o.id = :id")
    Optional<Order> findByIdWithItemsAndUser(@Param("id") Long id);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * Paginates the orders belonging to one authenticated user. The EntityGraph
     * eagerly loads items and their product so the mapper can build
     * {@code OrderItemResponse} without additional queries.
     */
    @EntityGraph(attributePaths = {"items", "items.product"})
    Page<Order> findByUserId(Long userId, Pageable pageable);

    /**
     * Returns the order with the given id only if it belongs to the given user.
     * Returns {@code Optional.empty()} otherwise, allowing the service to raise
     * a 404 without revealing whether the order exists for another user.
     */
    @Query("SELECT o FROM Order o JOIN FETCH o.items i LEFT JOIN FETCH i.product "
            + "WHERE o.id = :id AND o.user.id = :userId")
    Optional<Order> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    boolean existsByCustomerId(Long customerId);

    boolean existsByProductId(Long productId);
}
