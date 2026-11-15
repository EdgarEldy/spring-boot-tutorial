# Tests

Le projet comporte quatre niveaux de tests qui forment une pyramide : des tests unitaires
rapides à la base, des tests d'intégration plus lents au sommet. Chaque niveau utilise un
slice de test Spring différent pour ne charger que ce dont il a besoin.

---

## Pyramide de tests

```
         [Intégration]
         @SpringBootTest + Testcontainers
         Contexte Spring complet, PostgreSQL réel
         (SecurityAuthorizationTest, tests de cache)

      [Tests de contrôleurs]
      @WebMvcTest + @MockitoBean
      Couche MVC uniquement, pas de JPA, services mockés

   [Tests de repositories]
   @DataJpaTest + Testcontainers
   Couche JPA uniquement, schéma PostgreSQL réel via Flyway

[Tests unitaires]
@ExtendWith(MockitoExtension.class)
Pas de contexte Spring, Java pur + Mockito
```

---

## Tests unitaires

Les tests unitaires utilisent `@ExtendWith(MockitoExtension.class)`. Aucun contexte Spring
n'est démarré.

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

Ces tests sont les plus rapides et couvrent l'essentiel de la logique des services.

---

## Tests de repositories

Les tests de repositories utilisent `@DataJpaTest`, qui ne charge que la couche JPA (entités,
repositories, migrations Flyway). Un vrai conteneur PostgreSQL est démarré via Testcontainers.

```java
@DataJpaTest
@Import({ TestcontainersConfiguration.class, JpaAuditingConfig.class })
class CategoryRepositoryTest {
    @Autowired CategoryRepository categoryRepository;
    // ...
}
```

> **Important :** Importez toujours `JpaAuditingConfig` dans les classes `@DataJpaTest` qui
> créent des entités `Category`. Sans cela, `created_at` et `updated_at` ne sont jamais
> renseignés, provoquant une violation de contrainte NOT NULL à l'insertion. Le bug n'apparaît
> que lorsque la classe de test s'exécute en isolation - dans la suite complète, le contexte
> d'audit d'une autre classe fuit et masque le problème.

---

## Tests de contrôleurs

Les tests de contrôleurs utilisent `@WebMvcTest`, qui ne charge que la couche MVC. Les
services, les repositories et l'infrastructure de sécurité sont mockés.

```java
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CategoryService categoryService;

    // Requis car JwtAuthFilter a besoin de ces beans même dans un slice
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void findAll_returns200() throws Exception {
        // ...
    }
}
```

> **@WebMvcTest et JwtAuthFilter :** `JwtAuthFilter` étend `Filter`. Spring Boot inclut tous
> les beans `Filter` dans un contexte `@WebMvcTest` indépendamment de `addFilters = false`
> (ce flag contrôle l'exécution du filtre pendant la requête, pas sa construction).
> `JwtAuthFilter` a `JwtService` et `UserDetailsService` comme dépendances de constructeur,
> donc les deux doivent être présents dans le contexte via `@MockitoBean`. Si une branche
> ultérieure ajoute une dépendance de constructeur à `JwtAuthFilter`, toutes les classes
> `@WebMvcTest` doivent être mises à jour.

---

## Tests d'intégration

Les tests d'intégration utilisent `@SpringBootTest` avec un vrai conteneur PostgreSQL
Testcontainers. Ils testent la pile complète : HTTP, chaîne de filtres de sécurité, service,
repository, base de données.

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class SecurityAuthorizationTest {
    @Autowired TestRestTemplate restTemplate;
    // ...
}
```

`SecurityAuthorizationTest` vérifie le comportement de sécurité de bout en bout : inscription,
connexion, réception d'un vrai JWT, et utilisation de ce JWT pour appeler un endpoint protégé.
C'est le seul test qui exerce `JwtAuthFilter` avec un vrai token.

---

## Configuration Testcontainers

`TestcontainersConfiguration` démarre un `PostgreSQLContainer` partagé une fois par JVM :

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

Si Testcontainers ne trouve pas le socket Docker, relancez avec :

```bash
DOCKER_HOST=unix:///home/votreuser/.docker/desktop/docker.sock ./mvnw test
```

---

## Cache du contexte et isolation des tests

Spring réutilise les contextes d'application de test au sein de la même JVM. C'est une
optimisation, mais elle signifie que les entrées de cache ou les lignes de base de données
écrites par un test peuvent affecter un autre si elles ne sont pas nettoyées.

Pour les tests de cache (`CategoryServiceCacheTest`, `ProductServiceCacheTest`), nettoyez
le cache dans une méthode `@BeforeEach` :

```java
@BeforeEach
void clearCache() {
    cacheManager.getCache("categories").clear();
}
```

Pour les tests `@SpringBootTest(classes = {...})` qui listent des classes explicites,
revérifiez la liste chaque fois qu'un constructeur de service gagne une nouvelle dépendance.
Un test qui mockait trois dépendances échouera à construire le service après l'ajout d'une
quatrième.
