package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.dto.permission.PermissionRequest;
import com.edgareldy.springboottutorial.dto.permission.PermissionResponse;
import java.util.List;

/**
 * Contract for {@link com.edgareldy.springboottutorial.entity.Permission}
 * business operations. Controllers and tests depend on this interface,
 * never on its implementation directly.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface PermissionService {

    List<PermissionResponse> findAll();

    PermissionResponse create(PermissionRequest request);

    void delete(Long id);
}
