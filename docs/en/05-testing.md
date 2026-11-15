# Testing

The project has four levels of tests that form a pyramid: fast unit tests at the base,
slower integration tests at the top. Each level uses a different Spring test slice to
load only what is needed.

---

## Test pyramid

```
         [Integration]
         @SpringBootTest + Testcontainers
         Full Spring context, real PostgreSQL
         (SecurityAuthorizationTest, cache tests)

      [Controller tests]
      @WebMvcTest + @MockitoBean
      MVC layer only, no JPA, mocked services

   [Repository tests]
   @DataJpaTest + Testcontainers
   JPA layer only, real PostgreSQL schema via Flyway

[Unit tests]
@ExtendWith(MockitoExtension.class)
No Spring context, pure Java + Mockito
```

---

## Unit tests

Unit tests use `@ExtendWith(MockitoExtension.class)`. No Spring context is started.

```java
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock CategoryRepository categoryRepository;
    @Mock CategoryMapper categoryMapper;
    @InjectMocks CategoryServiceImpl categoryService;

    @Test
    void create_returnsMappedResponse() {
        // given
        when(categoryRepository.save(any())).thenReturn(category);
        when(categoryMapper.toResponse(category)).thenReturn(response);
        // when / then ...
    }
}
```

These tests are the fastest and should cover the bulk of service logic.

---

## Repository tests

Repository tests use `@DataJpaTest`, which loads only the JPA layer (entities, repositories,
Flyway migrations). A real PostgreSQL container is started via Testcontainers.

```java
@DataJpaTest
@Import({ TestcontainersConfiguration.class, JpaAuditingConfig.class })
class CategoryRepositoryTest {
    @Autowired CategoryRepository categoryRepository;
    // ...
}
```

> **Important:** Always import `JpaAuditingConfig` in `@DataJpaTest` classes that create
> `Category` entities. Without it, `created_at` and `updated_at` are never set, causing a
> NOT NULL constraint violation at insert time. The bug only appears when the test class runs
> in isolation - in the full suite, another class's context leaks the auditing configuration,
> masking the problem.

---

## Controller tests

Controller tests use `@WebMvcTest`, which loads only the MVC layer. Services, repositories,
and security infrastructure are mocked.

```java
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CategoryService categoryService;

    // Required because JwtAuthFilter needs these beans even in a slice
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void findAll_returns200() throws Exception {
        // ...
    }
}
```

> **@WebMvcTest and JwtAuthFilter:** `JwtAuthFilter` extends `Filter`. Spring Boot includes
> every `Filter` bean in a `@WebMvcTest` context regardless of `addFilters = false`. That flag
> controls whether the filter executes during the request, not whether it is instantiated.
> `JwtAuthFilter` has `JwtService` and `UserDetailsService` as constructor dependencies, so
> both must be present in the context via `@MockitoBean`. If a later branch adds a constructor
> dependency to `JwtAuthFilter`, every `@WebMvcTest` class must be updated.

---

## Integration tests

Integration tests use `@SpringBootTest` with a real Testcontainers PostgreSQL container.
They test the full stack: HTTP, security filter chain, service, repository, database.

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class SecurityAuthorizationTest {
    @Autowired TestRestTemplate restTemplate;
    // ...
}
```

`SecurityAuthorizationTest` verifies end-to-end security behaviour: register, login, receive
a real JWT, and use it to call a protected endpoint. This is the only test that exercises
`JwtAuthFilter` with a real token.

---

## Testcontainers configuration

`TestcontainersConfiguration` starts a shared `PostgreSQLContainer` once per JVM:

```java
@TestConfiguration
class TestcontainersConfiguration {
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
            .withReuse(true);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        // ...
    }
}
```

If Testcontainers cannot find the Docker socket, re-run with:

```bash
DOCKER_HOST=unix:///home/youruser/.docker/desktop/docker.sock ./mvnw test
```

---

## Context caching and test isolation

Spring reuses test application contexts across test classes in the same JVM. This is an
optimization, but it means cache entries or database rows written by one test can affect
another if not cleaned up.

For cache tests (`CategoryServiceCacheTest`, `ProductServiceCacheTest`), clean the cache
in a `@BeforeEach` method:

```java
@BeforeEach
void clearCache() {
    cacheManager.getCache("categories").clear();
}
```

For `@SpringBootTest(classes = {...})` tests that list explicit classes, revisit the list
whenever a service constructor gains a new dependency. A test that mocked three dependencies
will fail to construct the service after a fourth is added.
