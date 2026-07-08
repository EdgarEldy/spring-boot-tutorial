package edgareldy.springboottutorial.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import edgareldy.springboottutorial.dto.auth.RegisterRequest;
import edgareldy.springboottutorial.dto.category.CategoryRequest;
import edgareldy.springboottutorial.dto.customer.CustomerRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
    void listCategoriesIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
    }

    @Test
    void createCategoryRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Books"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCategoryIsForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .with(user("ada").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Books"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCategoryIsAllowedForAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Books"))))
                .andExpect(status().isCreated());
    }

    @Test
    void listCustomersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/customers")).andExpect(status().isUnauthorized());
    }

    @Test
    void listCustomersIsAllowedForAnyAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/customers").with(user("ada").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void createCustomerIsForbiddenForNonAdmin() throws Exception {
        CustomerRequest request = new CustomerRequest(
                "Ada", "Lovelace", "+1 202-555-0100", "ada@example.com", "1 Analytical Engine Way");

        mockMvc.perform(post("/api/v1/customers")
                        .with(user("ada").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCustomerIsAllowedForAdmin() throws Exception {
        CustomerRequest request = new CustomerRequest(
                "Grace", "Hopper", "+1 202-555-0101", "grace@example.com", "2 Compiler Street");

        mockMvc.perform(post("/api/v1/customers")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void registerIsPublic() throws Exception {
        RegisterRequest request = new RegisterRequest("turing", "turing@example.com", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }
}
