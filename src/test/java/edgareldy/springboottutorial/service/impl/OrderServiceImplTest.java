package edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edgareldy.springboottutorial.dto.common.PageResponse;
import edgareldy.springboottutorial.dto.order.OrderRequest;
import edgareldy.springboottutorial.dto.order.OrderResponse;
import edgareldy.springboottutorial.entity.Category;
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
        product = Product.builder().id(1L).category(category).productName("Keyboard").unitPrice(50.0f).build();
        order = Order.builder().id(1L).customer(customer).product(product).quantity(2).total(100.0).build();
        orderResponse = new OrderResponse(
                1L,
                new OrderResponse.CustomerSummary(1L, "Ada Lovelace"),
                new OrderResponse.ProductSummary(1L, "Keyboard", 50.0f),
                2,
                100.0);
    }

    @Test
    void findAllDelegatesToProjectedQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        OrderProjection projection = new OrderProjection(1L, 1L, "Ada Lovelace", 1L, "Keyboard", 50.0f, 2, 100.0);
        when(orderRepository.findAllProjected(null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(projection), pageable, 1));
        when(orderMapper.toResponse(projection)).thenReturn(orderResponse);

        PageResponse<OrderResponse> result = orderService.findAll(null, null, pageable);

        assertThat(result.content()).containsExactly(orderResponse);
    }

    @Test
    void findByIdReturnsResponseWhenFound() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        assertThat(orderService.findById(1L)).isEqualTo(orderResponse);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createComputesTotalAndPublishesEvent() {
        OrderRequest request = new OrderRequest(1L, 1L, 2);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderMapper.toEntity(request)).thenReturn(Order.builder().quantity(2).build());
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        assertThat(orderService.create(request)).isEqualTo(orderResponse);

        ArgumentCaptor<Order> savedOrderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(savedOrderCaptor.capture());
        assertThat(savedOrderCaptor.getValue().getTotal()).isEqualTo(100.0);
        assertThat(savedOrderCaptor.getValue().getCustomer()).isEqualTo(customer);
        assertThat(savedOrderCaptor.getValue().getProduct()).isEqualTo(product);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isEqualTo(new OrderCreatedEvent(1L, 1L, 1L, 2, 100.0));
    }

    @Test
    void createThrowsWhenCustomerMissing() {
        OrderRequest request = new OrderRequest(99L, 1L, 2);
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void createThrowsWhenProductMissing() {
        OrderRequest request = new OrderRequest(1L, 99L, 2);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateRecomputesTotal() {
        OrderRequest request = new OrderRequest(1L, 1L, 3);
        Order existing = Order.builder().id(1L).customer(customer).product(product).quantity(2).total(100.0).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(orderRepository.save(existing)).thenReturn(existing);
        when(orderMapper.toResponse(existing)).thenReturn(orderResponse);

        orderService.update(1L, request);

        assertThat(existing.getQuantity()).isEqualTo(3);
        assertThat(existing.getTotal()).isEqualTo(150.0);
    }

    @Test
    void updateThrowsWhenOrderMissing() {
        OrderRequest request = new OrderRequest(1L, 1L, 3);
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void deleteRemovesOrderWhenExists() {
        when(orderRepository.existsById(1L)).thenReturn(true);

        orderService.delete(1L);

        verify(orderRepository).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(orderRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> orderService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).deleteById(any());
    }
}
