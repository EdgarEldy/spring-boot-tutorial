package com.edgareldy.springboottutorial.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgareldy.springboottutorial.dto.auth.LoginRequest;
import com.edgareldy.springboottutorial.dto.auth.RegisterRequest;
import com.edgareldy.springboottutorial.dto.category.CategoryRequest;
import com.edgareldy.springboottutorial.dto.customer.CustomerRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End to end authorization checks against the real
 * {@code SecurityFilterChain}, real controllers and services, and a real
 * PostgreSQL instance via Testcontainers: no mocks, so the rules actually
 * enforced by {@code SecurityConfig} are what gets exercised, not a
 * per-controller assumption about them. Mirrors the access table in the
 * README's feature/auth section (categories/products public read, ADMIN
 * write; customers/orders authenticated read, ADMIN write; auth endpoints
 * public except {@code /me}).
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SecurityTestcontainersConfiguration.class)
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void _01_ShouldAllowAccess_WhenListingCategoriesAnonymously() throws Exception {
        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
    }

    @Test
    void _02_ShouldReturn401_WhenCreatingCategoryAnonymously() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Books"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void _03_ShouldReturn403_WhenNonAdminCreatesCategory() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .with(user("ada").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Books"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void _04_ShouldAllowAccess_WhenAdminCreatesCategory() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Books"))))
                .andExpect(status().isCreated());
    }

    @Test
    void _05_ShouldReturn401_WhenListingCustomersAnonymously() throws Exception {
        mockMvc.perform(get("/api/v1/customers")).andExpect(status().isUnauthorized());
    }

    @Test
    void _06_ShouldAllowAccess_WhenAuthenticatedUserListsCustomers() throws Exception {
        mockMvc.perform(get("/api/v1/customers").with(user("ada").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void _07_ShouldReturn403_WhenNonAdminCreatesCustomer() throws Exception {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way");

        mockMvc.perform(post("/api/v1/customers")
                        .with(user("ada").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void _08_ShouldAllowAccess_WhenAdminCreatesCustomer() throws Exception {
        CustomerRequest request = new CustomerRequest(
                "Grace", "Hopper", "+1 202-555-0101", "grace@example.com", "2 Compiler Street");

        mockMvc.perform(post("/api/v1/customers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void _09_ShouldAllowAccess_WhenRegisteringAnonymously() throws Exception {
        RegisterRequest request = new RegisterRequest("turing", "turing@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void _10_ShouldReturn401_WhenCallingMeAnonymously() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void _11_ShouldAllowAccess_WhenListingProductsAnonymously() throws Exception {
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk());
    }

    @Test
    void _12_ShouldReturn401_WhenListingOrdersAnonymously() throws Exception {
        mockMvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void _13_ShouldAllowAccess_WhenAuthenticatedUserListsOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders").with(user("ada").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void _14_ShouldAuthenticateSubsequentRequests_WhenLoginIssuesToken() throws Exception {
        RegisterRequest register = new RegisterRequest("hopper", "hopper@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("hopper", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("token").asText();

        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void _15_ShouldRejectLogin_WhenPasswordIsWrong() throws Exception {
        RegisterRequest register = new RegisterRequest("babbage", "babbage@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest wrongPassword = new LoginRequest("babbage", "not-the-password");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPassword)))
                .andExpect(status().isUnauthorized());
    }
}
