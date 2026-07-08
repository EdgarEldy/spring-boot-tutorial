package edgareldy.springboottutorial.service.impl;

import edgareldy.springboottutorial.dto.common.PageResponse;
import edgareldy.springboottutorial.dto.order.OrderRequest;
import edgareldy.springboottutorial.dto.order.OrderResponse;
import edgareldy.springboottutorial.entity.Customer;
import edgareldy.springboottutorial.entity.Order;
import edgareldy.springboottutorial.entity.Product;
import edgareldy.springboottutorial.event.OrderCreatedEvent;
import edgareldy.springboottutorial.exception.ResourceNotFoundException;
import edgareldy.springboottutorial.mapper.OrderMapper;
import edgareldy.springboottutorial.repository.CustomerRepository;
import edgareldy.springboottutorial.repository.OrderProjection;
import edgareldy.springboottutorial.repository.OrderRepository;
import edgareldy.springboottutorial.repository.ProductRepository;
import edgareldy.springboottutorial.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link OrderService} implementation backed by
 * {@link OrderRepository}.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PageResponse<OrderResponse> findAll(Long customerId, Long productId, Pageable pageable) {
        Page<OrderProjection> page = orderRepository.findAllProjected(customerId, productId, pageable);
        return PageResponse.from(page.map(orderMapper::toResponse));
    }

    @Override
    public OrderResponse findById(Long id) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + id));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse create(OrderRequest request) {
        Customer customer = getCustomerOrThrow(request.customerId());
        Product product = getProductOrThrow(request.productId());
        Order order = orderMapper.toEntity(request);
        order.setCustomer(customer);
        order.setProduct(product);
        order.setTotal(request.quantity() * product.getUnitPrice());
        Order saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderCreatedEvent(
                saved.getId(), customer.getId(), product.getId(), saved.getQuantity(), saved.getTotal()));
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse update(Long id, OrderRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + id));
        Customer customer = getCustomerOrThrow(request.customerId());
        Product product = getProductOrThrow(request.productId());
        order.setCustomer(customer);
        order.setProduct(product);
        order.setQuantity(request.quantity());
        order.setTotal(request.quantity() * product.getUnitPrice());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new ResourceNotFoundException("Order not found with id " + id);
        }
        orderRepository.deleteById(id);
    }

    private Customer getCustomerOrThrow(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + customerId));
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));
    }
}
