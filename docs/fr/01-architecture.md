# Architecture

Ce document décrit la structure de l'application, le chemin suivi par une requête à travers
les couches, et les décisions de conception qui s'appliquent à chaque fonctionnalité.

---

## Flux d'une requête

Chaque requête HTTP suit le même chemin à travers l'application :

```
Client HTTP
    |
    v
  Contrôleur  <----  AOP : ExecutionTimeAspect (@Around)
    |
    v
 Service (interface)
    |
    v
 ServiceImpl  <----  AOP : LoggingAspect (@Around, masque les champs sensibles)
    |
    v
 Repository   <----  AOP : RepositoryAuditAspect (@Before/@After/@AfterReturning/@AfterThrowing)
    |
    v
 PostgreSQL 16
```

Les aspects AOP s'exécutent en parallèle de chaque couche sans en modifier la logique.
La chaîne de filtres de sécurité s'intercale avant le contrôleur et prend en charge
l'authentification (voir [03-auth.md](03-auth.md)).

---

## Structure des packages

```
src/main/java/com/edgareldy/springboottutorial/
  config/           - Beans de configuration Spring (CacheConfig, CorsConfig, OpenApiConfig, ...)
  controller/       - Classes @RestController
  dto/
    common/         - ApiResponse<T>, PageResponse<T>
    category/       - CategoryRequest, CategoryResponse
    product/        - ProductRequest, ProductResponse
    customer/       - CustomerRequest, CustomerResponse
    order/          - OrderRequest, OrderResponse, ...
    auth/           - RegisterRequest, LoginRequest, AuthResponse, ...
    user/           - UserResponse, UpdateProfileRequest, ...
    role/           - RoleRequest, RoleResponse, UpdateRolePermissionsRequest
    permission/     - PermissionRequest, PermissionResponse
  entity/           - Entités JPA
    user/           - AppUser, Role, Permission, ActivationToken, ...
  exception/        - ResourceNotFoundException, BusinessRuleException, GlobalExceptionHandler
  mapper/           - Mappeurs MapStruct
  repository/       - Repositories Spring Data JPA
  security/         - JwtService, JwtAuthFilter, SecurityConfig, ...
  service/          - Interfaces de service
    impl/           - Implémentations des services
  aspect/           - LoggingAspect, ExecutionTimeAspect, RepositoryAuditAspect
  validation/       - Contraintes personnalisées @ValidPassword, @PasswordsMatch, @Unique
  event/            - OrderCreatedEvent, OrderCreatedEventListener
```

---

## Décisions de conception clés

### Contrat et implémentation pour les services

Chaque opération métier est déclarée dans une interface (`CategoryService`) et implémentée
dans une classe séparée (`CategoryServiceImpl`) dans le package `service/impl/`.

- Les contrôleurs et les tests injectent **l'interface**, jamais la classe d'implémentation.
- `@Transactional` est placé sur les méthodes de **l'implémentation**, pas sur l'interface.
- `@Service` est placé sur la classe d'implémentation.

Cette approche découple les appelants de la classe concrète, simplifie le mock dans les tests
et garde la frontière transactionnelle proche du code d'accès aux données.

### Enveloppe de réponse

Chaque réponse de contrôleur - succès ou erreur - est encapsulée dans `ApiResponse<T>` :

```java
public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    LocalDateTime timestamp
) {}
```

Les méthodes de fabrique `ApiResponse.success(message, data)` et `ApiResponse.error(message,
data)` assurent une construction cohérente. Le statut HTTP est communiqué via
`ResponseEntity<ApiResponse<T>>` plutôt que `@ResponseStatus`, ce qui donne aux contrôleurs
un contrôle explicite sur le code de statut.

Les listes paginées utilisent `PageResponse<T>` comme champ `data` :

```java
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean last
) {}
```

### DTOs en records Java

Tous les DTOs sont des types `record` Java. Les records sont immuables par nature, ce qui
élimine le risque de mutation accidentelle entre les couches. MapStruct génère le code de
mapping à la compilation à partir des noms des composants du record.

Les noms de champs JSON utilisent `@JsonProperty("snake_case")` sur les composants
individuels du record, découplant le contrat JSON de la convention de nommage Java :

```java
public record CategoryResponse(
    Long id,
    String name,
    @JsonProperty("created_at") LocalDateTime createdAt,
    @JsonProperty("updated_at") LocalDateTime updatedAt
) {}
```

### Format d'erreur - RFC 7807

Les réponses d'erreur utilisent la classe `ProblemDetail` native de Spring 6 comme champ
`data` de `ApiResponse`. ProblemDetail suit la RFC 7807 avec les champs `type`, `title`,
`status`, `detail` et `instance`. Les erreurs de validation (400) ajoutent une extension
`fieldErrors`.

### Gestion du schéma

`spring.jpa.hibernate.ddl-auto=validate` dans chaque profil. Hibernate valide uniquement que
le schéma correspond au modèle d'entités - il ne crée ni ne modifie jamais de tables. Flyway
est la source de vérité unique pour les changements de schéma.
