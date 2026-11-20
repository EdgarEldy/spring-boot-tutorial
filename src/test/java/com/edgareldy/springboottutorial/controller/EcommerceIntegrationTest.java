package com.edgareldy.springboottutorial.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.edgareldy.springboottutorial.dto.auth.LoginRequest;
import com.edgareldy.springboottutorial.dto.auth.RegisterRequest;
import com.edgareldy.springboottutorial.entity.Category;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.repository.CategoryRepository;
import com.edgareldy.springboottutorial.repository.ProductRepository;
import com.edgareldy.springboottutorial.repository.RoleRepository;
import java.math.BigDecimal;
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
 * End-to-end integration test covering the full e-commerce flow: product
 * catalogue, cart management, checkout, role-aware order access and status
 * lifecycle. Uses the real security stack and a Testcontainers PostgreSQL
 * instance. All scenario steps run in one test method to guarantee ordering
 * and share intermediate state (cart item id, order id).
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
class EcommerceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Long productId;
    private String tokenUserA;
    private String tokenUserB;
    private String tokenAdmin;

    @BeforeAll
    void setUp() throws Exception {
        Category category = categoryRepository.save(
                Category.builder().categoryName("IT_ECOM_Electronics").build());
        Product product = productRepository.save(Product.builder()
                .productName("IT_ECOM_Keyboard")
                .unitPrice(new BigDecimal("79.99"))
                .category(category)
                .active(true)
                .build());
        productId = product.getId();

        registerAndEnable("it_ecom_userA", "it_ecom_usera@test.com");
        registerAndEnable("it_ecom_userB", "it_ecom_userb@test.com");
        registerAndEnable("it_ecom_admin", "it_ecom_admin@test.com");

        appUserRepository.findByUsername("it_ecom_admin").ifPresent(u -> {
            roleRepository.findByRoleNameIgnoreCase("ADMIN").ifPresent(r -> {
                u.getRoles().add(r);
                appUserRepository.saveAndFlush(u);
            });
        });

        tokenUserA = login("it_ecom_usera@test.com");
        tokenUserB = login("it_ecom_userb@test.com");
        tokenAdmin = login("it_ecom_admin@test.com");
    }

    @Test
    void fullEcommerceFlow() throws Exception {
        // Step 1 - catalogue visible without authentication
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        // Step 2 - userA adds product to cart
        mockMvc.perform(post("/api/v1/cart/items")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"product_id\":" + productId + ",\"quantity\":2}"))
                .andExpect(status().isCreated());

        // Step 3 - userA views cart, verify item present
        MvcResult cartResult = mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andReturn();

        Long cartItemId = objectMapper
                .readTree(cartResult.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("id").asLong();

        // Step 4 - userA updates quantity
        mockMvc.perform(patch("/api/v1/cart/items/" + cartItemId)
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity").value(3));

        // Step 5 - checkout: cart is converted to order, cart is cleared
        MvcResult checkoutResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].product_name").value("IT_ECOM_Keyboard"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3))
                .andExpect(jsonPath("$.data.total_amount").value(239.97))
                .andReturn();

        Long orderIdUserA = objectMapper
                .readTree(checkoutResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Step 6 - cart is empty after checkout
        mockMvc.perform(get("/api/v1/cart")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());

        // Step 7 - userA sees only their own order
        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + orderIdUserA + ")]").exists());

        // Step 8 - userA can view order detail
        mockMvc.perform(get("/api/v1/orders/" + orderIdUserA)
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.items").isArray());

        // Step 9 - admin promotes order to CONFIRMED
        mockMvc.perform(patch("/api/v1/orders/" + orderIdUserA + "/status")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // Step 10 - userA sees the updated status
        mockMvc.perform(get("/api/v1/orders/" + orderIdUserA)
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // Step 11 - userB cannot access userA's order (ownership check returns 404)
        mockMvc.perform(get("/api/v1/orders/" + orderIdUserA)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());
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
