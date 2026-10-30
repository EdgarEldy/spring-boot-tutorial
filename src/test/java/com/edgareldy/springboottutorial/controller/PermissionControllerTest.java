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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgareldy.springboottutorial.config.MethodSecurityConfig;
import com.edgareldy.springboottutorial.dto.permission.PermissionRequest;
import com.edgareldy.springboottutorial.dto.permission.PermissionResponse;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import com.edgareldy.springboottutorial.exception.ResourceNotFoundException;
import com.edgareldy.springboottutorial.security.CustomPermissionEvaluator;
import com.edgareldy.springboottutorial.security.JwtService;
import com.edgareldy.springboottutorial.service.PermissionService;
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
 * MockMvc integration tests for {@link PermissionController}, with
 * {@link PermissionService} mocked. {@link MethodSecurityConfig} and
 * {@link CustomPermissionEvaluator} are imported so the class-level
 * {@code @PreAuthorize("hasRole('ADMIN')")} is genuinely enforced.
 * {@code JwtService} and {@code UserDetailsService} are mocked purely so
 * the context loads, since {@code @WebMvcTest} auto-includes
 * {@code JwtAuthFilter} as a {@code Filter} bean regardless of
 * {@code addFilters}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
@WebMvcTest(PermissionController.class)
@Import({MethodSecurityConfig.class, CustomPermissionEvaluator.class})
@AutoConfigureMockMvc(addFilters = false)
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private static PermissionResponse savedResponse() {
        return new PermissionResponse(1L, "PRODUCT", "WRITE");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _01_ShouldReturn200_WhenAdminListsPermissions() throws Exception {
        when(permissionService.findAll()).thenReturn(List.of(savedResponse()));

        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].resource").value("PRODUCT"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void _02_ShouldReturn403_WhenNonAdminListsPermissions() throws Exception {
        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _03_ShouldReturn201_WhenCreateRequestValid() throws Exception {
        PermissionRequest request = new PermissionRequest("PRODUCT", "WRITE");
        when(permissionService.create(any())).thenReturn(savedResponse());

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.action").value("WRITE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _04_ShouldReturn400_WhenFieldsAreBlank() throws Exception {
        PermissionRequest invalid = new PermissionRequest(" ", "");

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _05_ShouldReturn422_WhenPermissionAlreadyExists() throws Exception {
        PermissionRequest request = new PermissionRequest("PRODUCT", "WRITE");
        when(permissionService.create(any()))
                .thenThrow(new BusinessRuleException("Permission PRODUCT:WRITE already exists"));

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _06_ShouldReturn200_WhenDeleteSucceeds() throws Exception {
        mockMvc.perform(delete("/api/v1/permissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void _07_ShouldReturn403_WhenNonAdminDeletesPermission() throws Exception {
        mockMvc.perform(delete("/api/v1/permissions/1"))
                .andExpect(status().isForbidden());

        verify(permissionService, never()).delete(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _08_ShouldReturn422_WhenPermissionAssignedToRole() throws Exception {
        doThrow(new BusinessRuleException("Permission 1 is assigned to at least one role and cannot be deleted"))
                .when(permissionService).delete(eq(1L));

        mockMvc.perform(delete("/api/v1/permissions/1"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void _09_ShouldReturn404_WhenPermissionMissing() throws Exception {
        doThrow(new ResourceNotFoundException("Permission not found with id 99"))
                .when(permissionService).delete(eq(99L));

        mockMvc.perform(delete("/api/v1/permissions/99"))
                .andExpect(status().isNotFound());
    }
}
