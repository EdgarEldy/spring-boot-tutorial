package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.order.CustomerOrdersResponse;
import com.edgareldy.springboottutorial.dto.order.OrderCustomerResponse;
import com.edgareldy.springboottutorial.dto.order.OrderRequest;
import com.edgareldy.springboottutorial.dto.order.OrderResponse;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import com.edgareldy.springboottutorial.entity.Category;
import com.edgareldy.springboottutorial.entity.Customer;
import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.OrderStatus;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.event.OrderCreatedEvent;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.mapper.OrderMapper;
import com.edgareldy.springboottutorial.repository.CustomerRepository;
import com.edgareldy.springboottutorial.repository.OrderProjection;
import com.edgareldy.springboottutorial.repository.OrderRepository;
import com.edgareldy.springboottutorial.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for {@link OrderServiceImpl}, with {@link OrderRepository},
 * {@link CustomerRepository}, {@link ProductRepository}, {@link OrderMapper},
 * and {@link ApplicationEventPublisher} mocked.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Customer customer;
    private Product product;
    private Order order;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).firstName("Ada").lastName("Lovelace")
                .telephone("+1 202-555-0100").email("ada@example.com").address("1 Analytical Engine Way").build();
        Category category = Category.builder().id(1L).categoryName("Electronics").build();
        product = Product.builder().id(1L).category(category).productName("Keyboard")
                .unitPrice(new BigDecimal("50.00")).build();
        order = Order.builder().id(1L).customer(customer).product(product).quantity(2)
                .totalAmount(new BigDecimal("100.00")).orderNumber("ORD-HIST-000001")
                .status(OrderStatus.PENDING).build();
        OrderCustomerResponse customerResponse = new OrderCustomerResponse(
                1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way");
        ProductResponse productResponse = new ProductResponse(
                1L, "Keyboard", new BigDecimal("50.00"), null, true, 1L, "Electronics");
        orderResponse = new OrderResponse(1L, customerResponse, productResponse, 2,
                "ORD-HIST-000001", "PENDING", new BigDecimal("100.00"), List.of());
    }

    @Test
    void _01_ShouldDelegateToProjectedQuery_WhenListingOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        OrderProjection projection = new OrderProjection(
                1L, 1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way",
                1L, "Keyboard", new BigDecimal("50.00"), true, 1L, "Electronics",
                2, new BigDecimal("100.00"), OrderStatus.PENDING, "ORD-HIST-000001");
        when(orderRepository.findAllProjected(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 1));
        when(orderMapper.toResponse(projection)).thenReturn(orderResponse);

        PageResponse<OrderResponse> result = orderService.findAll(null, null, pageable);

        assertThat(result.content()).containsExactly(orderResponse);
    }

    @Test
    void _01b_ShouldGroupOrdersAndSumTotals_WhenListingByCustomer() {
        Pageable pageable = PageRequest.of(0, 10);
        Customer grace = Customer.builder().id(2L).firstName("Grace").lastName("Hopper")
                .telephone("+1 202-555-0101").email("grace@example.com").address("2 Compiler Street").build();
        OrderProjection adaKeyboard = new OrderProjection(
                1L, 1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way",
                1L, "Keyboard", new BigDecimal("19.99"), true, 1L, "Electronics",
                1, new BigDecimal("19.99"), OrderStatus.PENDING, "ORD-TEST-001");
        OrderProjection adaDesk = new OrderProjection(
                2L, 1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way",
                2L, "Desk", new BigDecimal("0.10"), true, 1L, "Electronics",
                2, new BigDecimal("0.20"), OrderStatus.PENDING, "ORD-TEST-002");
        OrderProjection graceDesk = new OrderProjection(
                3L, 2L, "Grace", "Hopper", "+1 202-555-0101", "grace@example.com", "2 Compiler Street",
                2L, "Desk", new BigDecimal("200.00"), true, 1L, "Electronics",
                1, new BigDecimal("200.00"), OrderStatus.PENDING, "ORD-TEST-003");
        CustomerOrdersResponse adaEntry = new CustomerOrdersResponse(null, List.of(), new BigDecimal("20.19"));
        CustomerOrdersResponse graceEntry = new CustomerOrdersResponse(null, List.of(), new BigDecimal("200.00"));
        when(orderRepository.findCustomersWithOrders(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(customer, grace), pageable, 2));
        when(orderRepository.findAllProjectedByCustomerIds(List.of(1L, 2L), null))
                .thenReturn(List.of(adaKeyboard, adaDesk, graceDesk));
        when(orderMapper.toCustomerOrdersResponse(customer, List.of(adaKeyboard, adaDesk), new BigDecimal("20.19")))
                .thenReturn(adaEntry);
        when(orderMapper.toCustomerOrdersResponse(grace, List.of(graceDesk), new BigDecimal("200.00")))
                .thenReturn(graceEntry);

        PageResponse<CustomerOrdersResponse> result = orderService.findAllGroupedByCustomer(null, null, pageable);

        assertThat(result.content()).containsExactly(adaEntry, graceEntry);
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void _01c_ShouldSkipOrderQuery_WhenNoCustomerMatches() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findCustomersWithOrders(99L, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<CustomerOrdersResponse> result = orderService.findAllGroupedByCustomer(99L, null, pageable);

        assertThat(result.content()).isEmpty();
        verify(orderRepository, never()).findAllProjectedByCustomerIds(any(), any());
    }

    @Test
    void _02_ShouldReturnResponse_WhenOrderFound() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        assertThat(orderService.findById(1L)).isEqualTo(orderResponse);
    }

    @Test
    void _03_ShouldThrowNotFound_WhenOrderMissing() {
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void _04_ShouldBuildOrderItemAndPublishEvent_WhenCreatingOrder() {
        OrderRequest request = new OrderRequest(1L, 1L, 2);
        Order savedOrder = Order.builder().id(1L).customer(customer).product(product).quantity(2)
                .totalAmount(new BigDecimal("100.00")).orderNumber("ORD-ABCD1234")
                .status(OrderStatus.PENDING).build();
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(orderResponse);

        assertThat(orderService.create(request)).isEqualTo(orderResponse);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order built = orderCaptor.getValue();
        assertThat(built.getCustomer()).isEqualTo(customer);
        assertThat(built.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(built.getItems()).hasSize(1);
        assertThat(built.getItems().get(0).getProductName()).isEqualTo("Keyboard");
        assertThat(built.getItems().get(0).getQuantity()).isEqualTo(2);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        OrderCreatedEvent event = eventCaptor.getValue();
        assertThat(event.orderId()).isEqualTo(1L);
        assertThat(event.customerId()).isEqualTo(1L);
        assertThat(event.productId()).isEqualTo(1L);
        assertThat(event.quantity()).isEqualTo(2);
        assertThat(event.totalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(event.orderNumber()).isNotBlank();
    }

    @Test
    void _05_ShouldThrowNotFound_WhenCustomerMissingOnCreate() {
        OrderRequest request = new OrderRequest(99L, 1L, 2);
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void _06_ShouldThrowNotFound_WhenProductMissingOnCreate() {
        OrderRequest request = new OrderRequest(1L, 99L, 2);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findByIdWithCategory(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void _07_ShouldRecomputeTotalAndReplaceItems_WhenUpdatingOrder() {
        OrderRequest request = new OrderRequest(1L, 1L, 3);
        Order existing = Order.builder().id(1L).customer(customer).product(product).quantity(2)
                .totalAmount(new BigDecimal("100.00")).orderNumber("ORD-HIST-000001")
                .status(OrderStatus.PENDING).build();
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findByIdWithCategory(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(existing)).thenReturn(existing);
        when(orderMapper.toResponse(existing)).thenReturn(orderResponse);

        orderService.update(1L, request);

        assertThat(existing.getQuantity()).isEqualTo(3);
        assertThat(existing.getTotalAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(existing.getItems()).hasSize(1);
        assertThat(existing.getItems().get(0).getQuantity()).isEqualTo(3);
        verify(orderRepository).save(existing);
    }

    @Test
    void _08_ShouldThrowNotFound_WhenOrderMissingOnUpdate() {
        OrderRequest request = new OrderRequest(1L, 1L, 3);
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void _09_ShouldRemoveOrder_WhenOrderExists() {
        when(orderRepository.existsById(1L)).thenReturn(true);

        orderService.delete(1L);

        verify(orderRepository).deleteById(1L);
    }

    @Test
    void _10_ShouldThrowNotFound_WhenOrderMissingOnDelete() {
        when(orderRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> orderService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).deleteById(any());
    }
}
