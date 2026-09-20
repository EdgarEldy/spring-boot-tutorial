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
import com.edgareldy.springboottutorial.mapper.CustomerMapper;
import com.edgareldy.springboottutorial.repository.CustomerRepository;
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
 * Unit tests for {@link CustomerServiceImpl}, with {@link CustomerRepository}
 * and {@link CustomerMapper} mocked.
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
                1L, "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way");
    }

    @Test
    void _01_ShouldUsePlainFindAll_WhenNoSearchTermGiven() {
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(customer), pageable, 1));
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        PageResponse<CustomerResponse> result = customerService.findAll(null, pageable);

        assertThat(result.content()).containsExactly(customerResponse);
        verify(customerRepository, never()).search(any(), any());
    }

    @Test
    void _02_ShouldUseSearchQuery_WhenSearchTermGiven() {
        Pageable pageable = PageRequest.of(0, 10);
        when(customerRepository.search("lovelace", pageable))
                .thenReturn(new PageImpl<>(List.of(customer), pageable, 1));
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        PageResponse<CustomerResponse> result = customerService.findAll("lovelace", pageable);

        assertThat(result.content()).containsExactly(customerResponse);
        verify(customerRepository, never()).findAll(pageable);
    }

    @Test
    void _03_ShouldThrowNotFound_WhenCustomerMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void _04_ShouldSaveCustomer_WhenEmailUnused() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way");
        when(customerRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(customerMapper.toEntity(request)).thenReturn(customer);
        when(customerRepository.save(customer)).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        assertThat(customerService.create(request)).isEqualTo(customerResponse);
    }

    @Test
    void _05_ShouldThrowBusinessRule_WhenEmailAlreadyUsed() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way");
        when(customerRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void _06_ShouldThrowBusinessRule_WhenEmailAlreadyUsedInDifferentCase() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Lovelace", "+1 202-555-0100", "ADA@EXAMPLE.COM", "1 Analytical Engine Way");
        when(customerRepository.existsByEmailIgnoreCase("ADA@EXAMPLE.COM")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void _07_ShouldApplyRequest_WhenEmailUnchanged() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Byron", "+1 202-555-0100", "ada@example.com", "2 Analytical Engine Way");
        CustomerResponse updatedResponse = new CustomerResponse(
                1L, "Ada", "Byron", "+1 202-555-0100", "ada@example.com", "2 Analytical Engine Way");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(updatedResponse);

        assertThat(customerService.update(1L, request)).isEqualTo(updatedResponse);

        verify(customerRepository, never()).existsByEmailIgnoreCase(any());
        verify(customerMapper).updateEntityFromRequest(request, customer);
    }

    @Test
    void _08_ShouldSkipEmailCheck_WhenOnlyCaseDiffers() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Byron", "+1 202-555-0100", "ADA@EXAMPLE.COM", "2 Analytical Engine Way");
        CustomerResponse updatedResponse = new CustomerResponse(
                1L, "Ada", "Byron", "+1 202-555-0100", "ADA@EXAMPLE.COM", "2 Analytical Engine Way");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(customer)).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(updatedResponse);

        assertThat(customerService.update(1L, request)).isEqualTo(updatedResponse);

        verify(customerRepository, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    void _09_ShouldThrowBusinessRule_WhenNewEmailUsedByAnotherCustomer() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Lovelace", "+1 202-555-0100", "ada.lovelace@example.com", "1 Analytical Engine Way");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailIgnoreCase("ada.lovelace@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(1L, request))
                .isInstanceOf(BusinessRuleException.class);

        verify(customerRepository, never()).save(any());
    }

    @Test
    void _10_ShouldThrowNotFound_WhenCustomerMissingOnUpdate() {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way");
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void _11_ShouldRemoveCustomer_WhenCustomerExists() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.delete(1L);

        verify(customerRepository).delete(customer);
    }

    @Test
    void _12_ShouldThrowNotFound_WhenCustomerMissingOnDelete() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
