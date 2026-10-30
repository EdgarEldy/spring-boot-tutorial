package com.edgareldy.springboottutorial.service;

import com.edgareldy.springboottutorial.dto.auth.UserResponse;
import com.edgareldy.springboottutorial.dto.user.AssignRolesRequest;
import com.edgareldy.springboottutorial.dto.user.UpdateProfileRequest;
import org.springframework.data.domain.Pageable;

/**
 * Business operations for the admin user-management endpoints. All mutating
 * operations (role assignment, delete) are restricted to the ADMIN role at
 * the controller level; the service itself does not re-check authority.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
public interface UserService {

    com.edgareldy.springboottutorial.dto.common.PageResponse<UserResponse> findAll(Pageable pageable);

    UserResponse findById(Long id);

    UserResponse assignRoles(Long userId, AssignRolesRequest request);

    void delete(Long id);

    UserResponse lock(Long id, String currentUsername);

    UserResponse unlock(Long id);

    UserResponse updateProfile(Long id, UpdateProfileRequest request);

    UserResponse activate(String token);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
}
