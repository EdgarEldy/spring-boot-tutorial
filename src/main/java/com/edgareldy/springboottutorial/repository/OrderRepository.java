package com.edgareldy.springboottutorial.repository;

import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            + "p.id, p.productName, p.unitPrice, cat.id, cat.categoryName, "
            + "o.quantity, o.total) "
            + "FROM Order o JOIN o.customer c JOIN o.product p JOIN p.category cat "
            + "WHERE (:customerId IS NULL OR c.id = :customerId) "
            + "AND (:productId IS NULL OR p.id = :productId)")
    Page<OrderProjection> findAllProjected(@Param("customerId") Long customerId,
                                            @Param("productId") Long productId,
                                            Pageable pageable);

    @Query("SELECT o FROM Order o JOIN FETCH o.customer JOIN FETCH o.product p JOIN FETCH p.category WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Order o JOIN o.product p JOIN FETCH p.category WHERE o.customer.id = :customerId")
    List<Product> findDistinctProductsByCustomerId(@Param("customerId") Long customerId);

    boolean existsByCustomerId(Long customerId);

    boolean existsByProductId(Long productId);
}
