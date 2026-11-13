package com.edgareldy.springboottutorial.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.customer.CustomerRequest;
import com.edgareldy.springboottutorial.dto.customer.CustomerResponse;
import com.edgareldy.springboottutorial.entity.Customer;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.dto.order.OrderedProductResponse;
import com.edgareldy.springboottutorial.mapper.CustomerMapper;
import com.edgareldy.springboottutorial.mapper.OrderMapper;
import com.edgareldy.springboottutorial.repository.CustomerRepository;
import com.edgareldy.springboottutorial.repository.OrderProjection;
import com.edgareldy.springboottutorial.repository.OrderRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Unit tests for {@link CustomerServiceImpl}, with {@link CustomerRepository},
 * {@link OrderRepository}, {@link CustomerMapper} and {@link OrderMapper} mocked.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private Customer customer;
    private CustomerResponse customerResponse;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L).firstName("Ada").lastName("Lovelace").telephone("+1 202-555-0100")
                .email("ada@example.com").address("1 Analytical Engine Way").build();
        customerResponse = new CustomerResponse(
                1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way", List.of(), 0.0);
    }

    @Test
    void _01_ShouldUsePlainFindAll_WhenNoSearchTermGiven() {
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(customer), pageable, 1));
        when(customerMapper.toResponseWithOrders(customer, List.of(), 0.0)).thenReturn(customerResponse);

        PageResponse<CustomerResponse> result = customerService.findAll(null, pageable);

        assertThat(result.content()).containsExactly(customerResponse);
        verify(customerRepository, never()).search(any(), any());
    }

    @Test
    void _02_ShouldUseSearchQuery_WhenSearchTermGiven() {
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.search("lovelace", pageable))
                .thenReturn(new PageImpl<>(List.of(customer), pageable, 1));
        when(customerMapper.toResponseWithOrders(customer, List.of(), 0.0)).thenReturn(customerResponse);

        PageResponse<CustomerResponse> result = customerService.findAll("lovelace", pageable);

        assertThat(result.content()).containsExactly(customerResponse);
        verify(customerRepository, never()).findAll(pageable);
    }

    @Test
    void _02b_ShouldAttachEachCustomerOrdersWithOneQuery_WhenListingCustomers() {
        Pageable pageable = PageRequest.of(0, 10);
        Customer grace = Customer.builder()
                .id(2L).firstName("Grace").lastName("Hopper").telephone("+1 202-555-0101")
                .email("grace@example.com").address("2 Compiler Street").build();
        OrderProjection adaKeyboard = new OrderProjection(
                1L, 1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way",
                1L, "Keyboard", 19.99f, 1L, "Electronics", 1, 19.99);
        OrderProjection adaDesk = new OrderProjection(
                2L, 1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way",
                2L, "Desk", 0.1f, 1L, "Electronics", 2, 0.2);
        List<OrderedProductResponse> adaLines = List.of(
                new OrderedProductResponse(1L, 1L, "Keyboard", 19.99f, 1, 19.99),
                new OrderedProductResponse(2L, 2L, "Desk", 0.1f, 2, 0.2));
        CustomerResponse adaResponse = new CustomerResponse(
                1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way",
                adaLines, 20.19);
        CustomerResponse graceResponse = new CustomerResponse(
                2L, "Grace", "Hopper", "+1 202-555-0101", "grace@example.com", "2 Compiler Street", List.of(), 0.0);
        when(customerRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(customer, grace), pageable, 2));
        when(orderRepository.findAllProjectedByCustomerIds(List.of(1L, 2L), null))
                .thenReturn(List.of(adaKeyboard, adaDesk));
        when(orderMapper.toOrderedProducts(List.of(adaKeyboard, adaDesk))).thenReturn(adaLines);
        when(orderMapper.toOrderedProducts(List.of())).thenReturn(List.of());
        // 19.99 + 0.2 is 20.189999999999998 in double arithmetic: the service must round it to 20.19.
        when(customerMapper.toResponseWithOrders(customer, adaLines, 20.19)).thenReturn(adaResponse);
        when(customerMapper.toResponseWithOrders(grace, List.of(), 0.0)).thenReturn(graceResponse);

        PageResponse<CustomerResponse> result = customerService.findAll(null, pageable);

        assertThat(result.content()).containsExactly(adaResponse, graceResponse);
        verify(orderRepository).findAllProjectedByCustomerIds(List.of(1L, 2L), null);
    }

    @Test
    void _02c_ShouldSkipOrderQuery_WhenPageIsEmpty() {
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(customerService.findAll(null, pageable).content()).isEmpty();

        verify(orderRepository, never()).findAllProjectedByCustomerIds(any(), any());
    }

    @Test
    void _03_ShouldReturnResponseWithOrderedProducts_WhenCustomerFound() {
        OrderProjection keyboardOrder = new OrderProjection(
                7L, 1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way",
                1L, "Keyboard", 79.99f, 1L, "Electronics", 2, 159.98);
        List<OrderedProductResponse> lines =
                List.of(new OrderedProductResponse(7L, 1L, "Keyboard", 79.99f, 2, 159.98));
        CustomerResponse detailResponse = new CustomerResponse(
                1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way",
                lines, 159.98);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.findAllProjectedByCustomerIds(List.of(1L), null)).thenReturn(List.of(keyboardOrder));
        when(orderMapper.toOrderedProducts(List.of(keyboardOrder))).thenReturn(lines);
        when(customerMapper.toResponseWithOrders(customer, lines, 159.98)).thenReturn(detailResponse);

        assertThat(customerService.findById(1L)).isEqualTo(detailResponse);
    }

    @Test
    void _04_ShouldThrowNotFound_WhenCustomerMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void _05_ShouldSaveCustomer_WhenEmailUnused() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way");
        when(customerRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerRepository.save(customer)).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        assertThat(customerService.create(request)).isEqualTo(customerResponse);
    }

    @Test
    void _06_ShouldThrowBusinessRule_WhenEmailAlreadyUsed() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way");
        when(customerRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void _07_ShouldThrowBusinessRule_WhenEmailAlreadyUsedInDifferentCase() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Lovelace", "+1 202-555-0100", "ADA@EXAMPLE.COM", "1 Analytical Engine Way");
        when(customerRepository.existsByEmailIgnoreCase("ADA@EXAMPLE.COM")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void _08_ShouldApplyRequest_WhenEmailUnchanged() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Byron", "+1 202-555-0100", "ada@example.com", "2 Analytical Engine Way");
        CustomerResponse updatedResponse = new CustomerResponse(
                1L, "Ada", "Byron", "+1 202-555-0100", "ada@example.com", "2 Analytical Engine Way", List.of(), 0.0);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);
        when(customerMapper.toResponseWithOrders(customer, List.of(), 0.0)).thenReturn(updatedResponse);

        assertThat(customerService.update(1L, request)).isEqualTo(updatedResponse);

        verify(customerRepository, never()).existsByEmailIgnoreCase(any());
        verify(customerMapper).updateEntityFromRequest(request, customer);
    }

    @Test
    void _09_ShouldSkipEmailCheck_WhenOnlyCaseDiffers() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Byron", "+1 202-555-0100", "ADA@EXAMPLE.COM", "2 Analytical Engine Way");
        CustomerResponse updatedResponse = new CustomerResponse(
                1L, "Ada", "Byron", "+1 202-555-0100", "ADA@EXAMPLE.COM", "2 Analytical Engine Way", List.of(), 0.0);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);
        when(customerMapper.toResponseWithOrders(customer, List.of(), 0.0)).thenReturn(updatedResponse);

        assertThat(customerService.update(1L, request)).isEqualTo(updatedResponse);

        verify(customerRepository, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    void _10_ShouldThrowBusinessRule_WhenNewEmailUsedByAnotherCustomer() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Lovelace", "+1 202-555-0100", "ada.lovelace@example.com", "1 Analytical Engine Way");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailIgnoreCase("ada.lovelace@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(1L, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void _11_ShouldThrowNotFound_WhenCustomerMissingOnUpdate() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way");
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void _12_ShouldRemoveCustomer_WhenCustomerExists() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.existsByCustomerId(1L)).thenReturn(false);

        customerService.delete(1L);

        verify(customerRepository).delete(customer);
    }

    @Test
    void _13_ShouldThrowNotFound_WhenCustomerMissingOnDelete() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void _14_ShouldThrowBusinessRule_WhenCustomerHasExistingOrders() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.existsByCustomerId(1L)).thenReturn(true);

        assertThatThrownBy(() -> customerService.delete(1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(customerRepository, never()).delete(any());
    }
}
