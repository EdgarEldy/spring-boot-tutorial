package edgareldy.springboottutorial.service;

import edgareldy.springboottutorial.dto.common.PageResponse;
import edgareldy.springboottutorial.dto.order.OrderRequest;
import edgareldy.springboottutorial.dto.order.OrderResponse;
import org.springframework.data.domain.Pageable;

/**
 * Contract for {@link edgareldy.springboottutorial.entity.Order} business
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

    OrderResponse findById(Long id);

    OrderResponse create(OrderRequest request);

    OrderResponse update(Long id, OrderRequest request);

    void delete(Long id);
}
