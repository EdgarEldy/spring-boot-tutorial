package com.edgareldy.springboottutorial.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgareldy.springboottutorial.dto.auth.LoginRequest;
import com.edgareldy.springboottutorial.dto.auth.RegisterRequest;
import com.edgareldy.springboottutorial.dto.order.OrderStatusUpdateRequest;
import com.edgareldy.springboottutorial.entity.Order;
import com.edgareldy.springboottutorial.entity.OrderStatus;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.repository.OrderRepository;
import com.edgareldy.springboottutorial.repository.RoleRepository;
import java.math.BigDecimal;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * End-to-end authorization tests for the order endpoints, using the real
 * security stack and a Testcontainers PostgreSQL instance. Three actors are
 * seeded once per class: userA and userB (ROLE_USER) each own one order;
 * admin (ROLE_ADMIN) owns none. The tests verify that ownership checks return
 * 404 (never 403) when a user requests another user's order, and that status
 * transitions are enforced by the service layer.
 * <p>
 * Created edgar.muhamyangabo on 11/20/26
 * Author : edgar.muhamyangabo
 * Date : 11/20/26
 * Project : spring-boot-tutorial
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(SecurityTestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrderRepository orderRepository;

    private String tokenUserA;
    private String tokenUserB;
    private String tokenAdmin;
    private Long orderIdUserA;
    private Long orderIdUserB;

    @BeforeAll
    void setUp() throws Exception {
        registerAndEnable("userA_auth", "usera@authtest.com");
        registerAndEnable("userB_auth", "userb@authtest.com");
        registerAndEnable("admin_auth", "admin@authtest.com");

        appUserRepository.findByUsername("admin_auth").ifPresent(u -> {
            roleRepository.findByRoleNameIgnoreCase("ADMIN").ifPresent(adminRole -> {
                u.getRoles().add(adminRole);
                appUserRepository.saveAndFlush(u);
            });
        });

        tokenUserA = login("usera@authtest.com");
        tokenUserB = login("userb@authtest.com");
        tokenAdmin = login("admin@authtest.com");

        AppUser userA = appUserRepository.findByUsername("userA_auth").orElseThrow();
        AppUser userB = appUserRepository.findByUsername("userB_auth").orElseThrow();

        orderIdUserA = orderRepository.save(Order.builder()
                .user(userA).status(OrderStatus.PENDING)
                .orderNumber("ORD-AUTH-TEST-A")
                .totalAmount(new BigDecimal("50.00")).quantity(1).build()).getId();

        orderIdUserB = orderRepository.save(Order.builder()
                .user(userB).status(OrderStatus.CONFIRMED)
                .orderNumber("ORD-AUTH-TEST-B")
                .totalAmount(new BigDecimal("80.00")).quantity(2).build()).getId();
    }

    @Test
    void _01_UserA_CanSeeOnlyOwnOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders").header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.data.content[0].order_number").value("ORD-AUTH-TEST-A"));
    }

    @Test
    void _02_UserA_CannotSeeUserB_Order() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + orderIdUserB)
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNotFound());
    }

    @Test
    void _03_Admin_CanSeeAllOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total_elements",
                        Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void _04_Admin_CanSeeAnyOrderById() throws Exception {
        mockMvc.perform(get("/api/v1/orders/" + orderIdUserA)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.order_number").value("ORD-AUTH-TEST-A"));
    }

    @Test
    void _05_UserA_CannotUpdateStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/" + orderIdUserA + "/status")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderStatusUpdateRequest(OrderStatus.CONFIRMED))))
                .andExpect(status().isForbidden());
    }

    @Test
    void _06_Admin_CanUpdateStatus_ValidTransition() throws Exception {
        // orderIdUserA starts as PENDING; PENDING -> CONFIRMED is a valid transition.
        mockMvc.perform(patch("/api/v1/orders/" + orderIdUserA + "/status")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderStatusUpdateRequest(OrderStatus.CONFIRMED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void _07_Admin_CannotUpdateStatus_InvalidTransition() throws Exception {
        // orderIdUserB is already CONFIRMED; CONFIRMED -> PENDING is not allowed.
        mockMvc.perform(patch("/api/v1/orders/" + orderIdUserB + "/status")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new OrderStatusUpdateRequest(OrderStatus.PENDING))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void _08_UnauthenticatedUser_CannotAccessOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized());
    }

    private void registerAndEnable(String username, String email) throws Exception {
        RegisterRequest req = new RegisterRequest(
                "Test", "User", username, email, "Password1@", "Password1@");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
        appUserRepository.findByUsername(username).ifPresent(u -> {
            u.setEnabled(true);
            appUserRepository.saveAndFlush(u);
        });
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, "Password1@"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("access_token").asText();
    }
}
