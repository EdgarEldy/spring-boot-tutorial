package com.edgareldy.springboottutorial.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgareldy.springboottutorial.dto.auth.UserResponse;
import org.springframework.security.test.context.support.WithMockUser;
import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.role.RoleResponse;
import com.edgareldy.springboottutorial.dto.user.AssignRolesRequest;
import com.edgareldy.springboottutorial.dto.user.UpdateProfileRequest;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.security.JwtService;
import com.edgareldy.springboottutorial.service.BlacklistedTokenService;
import com.edgareldy.springboottutorial.service.UserService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc integration tests for {@link UserController}, with
 * {@link UserService} mocked. Security filters are disabled: ADMIN-only
 * authorization is SecurityConfig's responsibility and is covered separately.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    // Required for JwtAuthFilter to construct even when filters are disabled.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private BlacklistedTokenService blacklistedTokenService;

    @Test
    void _01_ShouldReturn200WithPage_WhenFindingAll() throws Exception {
        UserResponse userResponse = new UserResponse(1L, "ada", "ada@example.com", true,
                List.of(new RoleResponse(1L, "USER", List.of())));
        PageResponse<UserResponse> page = PageResponse.from(
                new org.springframework.data.domain.PageImpl<>(List.of(userResponse)));
        when(userService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value("ada"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void _02_ShouldReturn200_WhenFindingById() throws Exception {
        UserResponse userResponse = new UserResponse(1L, "ada", "ada@example.com", true,
                List.of(new RoleResponse(1L, "USER", List.of())));
        when(userService.findById(1L)).thenReturn(userResponse);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("ada"));
    }

    @Test
    void _03_ShouldReturn404_WhenUserNotFoundOnFindById() throws Exception {
        when(userService.findById(99L)).thenThrow(new ResourceNotFoundException("User not found with id 99"));

        mockMvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void _04_ShouldReturn200_WhenAssigningRoles() throws Exception {
        UserResponse updated = new UserResponse(1L, "ada", "ada@example.com", true,
                List.of(new RoleResponse(1L, "USER", List.of()), new RoleResponse(2L, "ADMIN", List.of())));
        when(userService.assignRoles(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignRolesRequest(Set.of(1L, 2L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void _05_ShouldReturn400_WhenRoleIdsNullOnAssignRoles() throws Exception {
        mockMvc.perform(put("/api/v1/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role_ids\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void _06_ShouldReturn422_WhenSomeRoleIdsInvalid() throws Exception {
        when(userService.assignRoles(eq(1L), any()))
                .thenThrow(new BusinessRuleException("One or more role IDs are invalid"));

        mockMvc.perform(put("/api/v1/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignRolesRequest(Set.of(99L)))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void _07_ShouldReturn200_WhenDeletingUser() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void _08_ShouldReturn404_WhenUserNotFoundOnDelete() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id 99"))
                .when(userService).delete(99L);

        mockMvc.perform(delete("/api/v1/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void _09_ShouldReturn200_WhenLockingUser() throws Exception {
        UserResponse locked = new UserResponse(1L, "target", "target@example.com", true,
                List.of(new RoleResponse(1L, "USER", List.of())));
        when(userService.lock(eq(1L), eq("admin"))).thenReturn(locked);

        mockMvc.perform(patch("/api/v1/users/1/lock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("target"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void _10_ShouldReturn422_WhenAdminLocksOwnAccount() throws Exception {
        when(userService.lock(eq(1L), any()))
                .thenThrow(new BusinessRuleException("Admin cannot lock their own account"));

        mockMvc.perform(patch("/api/v1/users/1/lock"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void _11_ShouldReturn200_WhenUnlockingUser() throws Exception {
        UserResponse unlocked = new UserResponse(1L, "target", "target@example.com", true,
                List.of(new RoleResponse(1L, "USER", List.of())));
        when(userService.unlock(1L)).thenReturn(unlocked);

        mockMvc.perform(patch("/api/v1/users/1/unlock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("target"));
    }

    @Test
    void _12_ShouldReturn200_WhenUpdatingProfile() throws Exception {
        UserResponse updated = new UserResponse(1L, "newname", "new@example.com", true,
                List.of(new RoleResponse(1L, "USER", List.of())));
        when(userService.updateProfile(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateProfileRequest("newname", "new@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("newname"));
    }

    @Test
    void _13_ShouldReturn422_WhenUsernameAlreadyUsed() throws Exception {
        when(userService.updateProfile(eq(1L), any()))
                .thenThrow(new BusinessRuleException("Username newname is already in use"));

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateProfileRequest("newname", null))))
                .andExpect(status().isUnprocessableEntity());
    }
}
