package edgareldy.springboottutorial.service;

import edgareldy.springboottutorial.dto.common.PageResponse;
import edgareldy.springboottutorial.dto.customer.CustomerRequest;
import edgareldy.springboottutorial.dto.customer.CustomerResponse;
import org.springframework.data.domain.Pageable;

/**
 * Contract for {@link edgareldy.springboottutorial.entity.Customer} business
 * operations. Controllers and tests depend on this interface, never on its
 * implementation directly.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public interface CustomerService {

    PageResponse<CustomerResponse> findAll(String search, Pageable pageable);

    CustomerResponse findById(Long id);

    CustomerResponse create(CustomerRequest request);

    CustomerResponse update(Long id, CustomerRequest request);

    void delete(Long id);
}
