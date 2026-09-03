package com.edgareldy.springboottutorial.service.impl;

import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.customer.CustomerRequest;
import com.edgareldy.springboottutorial.dto.customer.CustomerResponse;
import com.edgareldy.springboottutorial.dto.product.ProductResponse;
import com.edgareldy.springboottutorial.entity.Customer;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.mapper.CustomerMapper;
import com.edgareldy.springboottutorial.mapper.ProductMapper;
import com.edgareldy.springboottutorial.repository.CustomerRepository;
import com.edgareldy.springboottutorial.repository.OrderRepository;
import com.edgareldy.springboottutorial.service.CustomerService;
import java.util.List;
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
    private final ProductMapper productMapper;

    @Override
    public PageResponse<CustomerResponse> findAll(String search, Pageable pageable) {
        Page<Customer> page = StringUtils.hasText(search)
                ? customerRepository.search(search, pageable)
                : customerRepository.findAll(pageable);
        return PageResponse.from(page.map(customerMapper::toResponse));
    }

    @Override
    public CustomerResponse findById(Long id) {
        Customer customer = getCustomerOrThrow(id);
        List<ProductResponse> orderedProducts = orderRepository.findDistinctProductsByCustomerId(id).stream()
                .map(productMapper::toResponse)
                .toList();
        return customerMapper.toDetailResponse(customer, orderedProducts);
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
        return customerMapper.toResponse(customerRepository.save(customer));
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

    private Customer getCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + id));
    }
}
