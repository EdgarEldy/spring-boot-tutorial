package edgareldy.springboottutorial.repository;

import edgareldy.springboottutorial.entity.Order;
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

    @Query("SELECT new edgareldy.springboottutorial.repository.OrderProjection("
            + "o.id, c.id, CONCAT(c.firstName, ' ', c.lastName), p.id, p.productName, p.unitPrice, o.quantity, o.total) "
            + "FROM Order o JOIN o.customer c JOIN o.product p "
            + "WHERE (:customerId IS NULL OR c.id = :customerId) "
            + "AND (:productId IS NULL OR p.id = :productId)")
    Page<OrderProjection> findAllProjected(@Param("customerId") Long customerId,
                                            @Param("productId") Long productId,
                                            Pageable pageable);

    @Query("SELECT o FROM Order o JOIN FETCH o.customer JOIN FETCH o.product WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);
}
