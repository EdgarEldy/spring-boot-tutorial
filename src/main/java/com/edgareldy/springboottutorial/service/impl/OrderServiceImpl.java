package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.order.CustomerOrdersResponse;
import com.edgareldy.springboottutorial.dto.order.OrderRequest;
import com.edgareldy.springboottutorial.dto.order.OrderResponse;
import com.edgareldy.springboottutorial.entity.Customer;
import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.OrderItem;
import com.edgareldy.springboottutorial.entity.OrderStatus;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.event.OrderCreatedEvent;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.mapper.OrderMapper;
import com.edgareldy.springboottutorial.repository.CustomerRepository;
import com.edgareldy.springboottutorial.repository.OrderProjection;
import com.edgareldy.springboottutorial.repository.OrderRepository;
import com.edgareldy.springboottutorial.repository.ProductRepository;
import com.edgareldy.springboottutorial.service.OrderService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
    public PageResponse<CustomerOrdersResponse> findAllGroupedByCustomer(
            Long customerId, Long productId, Pageable pageable) {
        Page<Customer> customers = orderRepository.findCustomersWithOrders(customerId, productId, pageable);
        List<Long> customerIds = customers.getContent().stream().map(Customer::getId).toList();
        Map<Long, List<OrderProjection>> ordersByCustomer = customerIds.isEmpty()
                ? Map.of()
                : orderRepository.findAllProjectedByCustomerIds(customerIds, productId).stream()
                        .collect(Collectors.groupingBy(OrderProjection::customerId));
        return PageResponse.from(customers.map(customer -> {
            List<OrderProjection> orders = ordersByCustomer.getOrDefault(customer.getId(), List.of());
            BigDecimal total = orders.stream()
                    .map(OrderProjection::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return orderMapper.toCustomerOrdersResponse(customer, orders, total);
        }));
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
        BigDecimal subtotal = product.getUnitPrice().multiply(BigDecimal.valueOf(request.quantity()));
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        OrderItem item = OrderItem.builder()
                .product(product)
                .productName(product.getProductName())
                .unitPrice(product.getUnitPrice())
                .quantity(request.quantity())
                .subtotal(subtotal)
                .build();

        Order order = Order.builder()
                .customer(customer)
                .product(product)
                .quantity(request.quantity())
                .totalAmount(subtotal)
                .orderNumber(orderNumber)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>(List.of(item)))
                .build();
        item.setOrder(order);

        Order saved = orderRepository.save(order);
        eventPublisher.publishEvent(new OrderCreatedEvent(
                saved.getId(), customer.getId(), product.getId(),
                saved.getQuantity(), saved.getTotalAmount(), saved.getOrderNumber()));
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse update(Long id, OrderRequest request) {
        Order order = orderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + id));
        Customer customer = getCustomerOrThrow(request.customerId());
        Product product = getProductOrThrow(request.productId());
        BigDecimal subtotal = product.getUnitPrice().multiply(BigDecimal.valueOf(request.quantity()));

        OrderItem item = OrderItem.builder()
                .product(product)
                .productName(product.getProductName())
                .unitPrice(product.getUnitPrice())
                .quantity(request.quantity())
                .subtotal(subtotal)
                .build();

        order.setCustomer(customer);
        order.setProduct(product);
        order.setQuantity(request.quantity());
        order.setTotalAmount(subtotal);
        order.getItems().clear();
        order.getItems().add(item);
        item.setOrder(order);

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
        return productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + productId));
    }
}
