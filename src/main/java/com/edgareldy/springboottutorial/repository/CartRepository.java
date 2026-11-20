package com.edgareldy.springboottutorial.repository;

import com.edgareldy.springboottutorial.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link Cart} entities. The two finder methods
 * both eager-load the items and their associated products in a single query,
 * preventing LazyInitializationException when the mapper accesses the item
 * list outside the original persistence context.
 * <p>
 * Created edgar.muhamyangabo on 11/19/26
 * Author : edgar.muhamyangabo
 * Date : 11/19/26
 * Project : spring-boot-tutorial
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserUsername(String username);
}
