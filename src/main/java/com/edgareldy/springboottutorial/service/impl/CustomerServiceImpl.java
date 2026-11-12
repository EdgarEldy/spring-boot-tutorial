package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.customer.CustomerRequest;
import com.edgareldy.springboottutorial.dto.customer.CustomerResponse;
import com.edgareldy.springboottutorial.entity.Customer;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.mapper.CustomerMapper;
import com.edgareldy.springboottutorial.mapper.OrderMapper;
import com.edgareldy.springboottutorial.repository.CustomerRepository;
import com.edgareldy.springboottutorial.repository.OrderProjection;
import com.edgareldy.springboottutorial.repository.OrderRepository;
import com.edgareldy.springboottutorial.service.CustomerService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Default {@link CustomerService} implementation backed by
 * {@link CustomerRepository}.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    public PageResponse<CustomerResponse> findAll(String search, Pageable pageable) {
        Page<Customer> page = StringUtils.hasText(search)
                ? customerRepository.search(search, pageable)
                : customerRepository.findAll(pageable);
        List<Long> customerIds = page.getContent().stream().map(Customer::getId).toList();
        Map<Long, List<OrderProjection>> ordersByCustomer = findOrdersByCustomer(customerIds);
        return PageResponse.from(page.map(customer ->
                toResponseWithOrders(customer, ordersByCustomer.getOrDefault(customer.getId(), List.of()))));
    }

    @Override
    public CustomerResponse findById(Long id) {
        return toResponseWithOrders(getCustomerOrThrow(id));
    }

    @Override
    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        if (customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleException("Email " + request.email() + " is already in use");
        }
        Customer customer = customerMapper.toEntity(request);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = getCustomerOrThrow(id);
        if (!customer.getEmail().equalsIgnoreCase(request.email())
                && customerRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleException("Email " + request.email() + " is already in use");
        }
        customerMapper.updateEntityFromRequest(request, customer);
        return toResponseWithOrders(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Customer customer = getCustomerOrThrow(id);
        if (orderRepository.existsByCustomerId(id)) {
            throw new BusinessRuleException("Customer " + id + " has existing orders and cannot be deleted");
        }
        customerRepository.delete(customer);
    }

    /**
     * Loads the orders of every given customer with one query, whatever the
     * number of customers, and groups them by customer id.
     */
    private Map<Long, List<OrderProjection>> findOrdersByCustomer(List<Long> customerIds) {
        if (customerIds.isEmpty()) {
            return Map.of();
        }
        return orderRepository.findAllProjectedByCustomerIds(customerIds, null).stream()
                .collect(Collectors.groupingBy(OrderProjection::customerId));
    }

    private CustomerResponse toResponseWithOrders(Customer customer) {
        List<Long> customerIds = List.of(customer.getId());
        return toResponseWithOrders(
                customer, findOrdersByCustomer(customerIds).getOrDefault(customer.getId(), List.of()));
    }

    private CustomerResponse toResponseWithOrders(Customer customer, List<OrderProjection> orders) {
        double total = orders.stream().mapToDouble(OrderProjection::total).sum();
        // Rounded to cents: summing doubles can leave noise such as 3949.9700000000003.
        return customerMapper.toResponseWithOrders(
                customer, orderMapper.toOrderedProducts(orders), Math.round(total * 100) / 100.0);
    }

    private Customer getCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));
    }
}
