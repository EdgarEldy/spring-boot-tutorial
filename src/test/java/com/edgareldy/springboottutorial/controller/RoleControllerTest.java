package com.edgareldy.springboottutorial.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgareldy.springboottutorial.config.MethodSecurityConfig;
import com.edgareldy.springboottutorial.dto.common.PageResponse;
import com.edgareldy.springboottutorial.dto.permission.PermissionResponse;
import com.edgareldy.springboottutorial.dto.role.RoleRequest;
import com.edgareldy.springboottutorial.dto.role.RoleResponse;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.security.CustomPermissionEvaluator;
import com.edgareldy.springboottutorial.security.JwtService;
import com.edgareldy.springboottutorial.service.BlacklistedTokenService;
import com.edgareldy.springboottutorial.service.RoleService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MockMvc integration tests for {@link RoleController}, with
 * {@link RoleService} mocked. {@link MethodSecurityConfig} and
 * {@link CustomPermissionEvaluator} are imported so the class-level
 * {@code @PreAuthorize("hasRole('ADMIN')")} is genuinely enforced.
 * {@code JwtService} and {@code UserDetailsService} are mocked so
 * the context loads with {@code JwtAuthFilter} present.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@WebMvcTest(RoleController.class)
@Import({MethodSecurityConfig.class, CustomPermissionEvaluator.class})
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private BlacklistedTokenService blacklistedTokenService;

    private static RoleResponse savedResponse() {
        return new RoleResponse(1L, "ADMIN", List.of(new PermissionResponse(1L, "PRODUCT", "WRITE")));
    }

    private static PageResponse<RoleResponse> pagedResponse() {
        return new PageResponse<>(List.of(savedResponse()), 0, 20, 1, 1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _01_ShouldReturn200_WhenAdminListsRoles() throws Exception {
        when(roleService.findAll(any())).thenReturn(pagedResponse());

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].role_name").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void _02_ShouldReturn403_WhenNonAdminListsRoles() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _03_ShouldReturn201_WhenCreateRequestValid() throws Exception {
        RoleRequest request = new RoleRequest("ADMIN");
        when(roleService.create(any())).thenReturn(savedResponse());

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role_name").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _04_ShouldReturn400_WhenRoleNameBlank() throws Exception {
        RoleRequest invalid = new RoleRequest(" ");

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _05_ShouldReturn422_WhenRoleNameAlreadyExists() throws Exception {
        RoleRequest request = new RoleRequest("ADMIN");
        when(roleService.create(any()))
                .thenThrow(new BusinessRuleException("Role name ADMIN already exists"));

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "USER")
    void _06_ShouldReturn403_WhenNonAdminCreatesRole() throws Exception {
        RoleRequest request = new RoleRequest("ADMIN");

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(roleService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _07_ShouldReturn200_WhenUpdateSucceeds() throws Exception {
        RoleRequest request = new RoleRequest("MODERATOR");
        RoleResponse updated = new RoleResponse(1L, "MODERATOR", List.of());
        when(roleService.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role_name").value("MODERATOR"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _08_ShouldReturn404_WhenUpdatingMissingRole() throws Exception {
        RoleRequest request = new RoleRequest("MODERATOR");
        when(roleService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Role not found with id 99"));

        mockMvc.perform(put("/api/v1/roles/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _09_ShouldReturn200_WhenDeleteSucceeds() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void _10_ShouldReturn403_WhenNonAdminDeletesRole() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/1"))
                .andExpect(status().isForbidden());

        verify(roleService, never()).delete(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _11_ShouldReturn404_WhenDeletingMissingRole() throws Exception {
        doThrow(new ResourceNotFoundException("Role not found with id 99"))
                .when(roleService).delete(eq(99L));

        mockMvc.perform(delete("/api/v1/roles/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _12_ShouldReturn200_WhenAssignPermissionSucceeds() throws Exception {
        when(roleService.assignPermission(1L, 1L)).thenReturn(savedResponse());

        mockMvc.perform(post("/api/v1/roles/1/permissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role_name").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _13_ShouldReturn422_WhenPermissionAlreadyAssigned() throws Exception {
        when(roleService.assignPermission(1L, 1L))
                .thenThrow(new BusinessRuleException("Permission 1 is already assigned to role 1"));

        mockMvc.perform(post("/api/v1/roles/1/permissions/1"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _14_ShouldReturn200_WhenRemovePermissionSucceeds() throws Exception {
        when(roleService.removePermission(1L, 1L)).thenReturn(new RoleResponse(1L, "ADMIN", List.of()));

        mockMvc.perform(delete("/api/v1/roles/1/permissions/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _15_ShouldReturn422_WhenPermissionNotAssigned() throws Exception {
        when(roleService.removePermission(1L, 1L))
                .thenThrow(new BusinessRuleException("Permission 1 is not assigned to role 1"));

        mockMvc.perform(delete("/api/v1/roles/1/permissions/1"))
                .andExpect(status().isUnprocessableEntity());
    }
}
