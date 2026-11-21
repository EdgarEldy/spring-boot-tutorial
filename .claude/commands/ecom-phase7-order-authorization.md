# Phase 7 — feature/order-authorization (autorisations commandes)

Crée la branche `feature/order-authorization` depuis `feature/order-notifications`.
Finalise les contrôles de propriété et les autorisations sur les commandes et le panier.

## Prérequis

- Phase 6 terminée (`feature/order-notifications` complète)

## Instructions pour l'agent

### Étape 0 — Vérification Git

```bash
git log --oneline feature/order-notifications -1
git branch | grep order-authorization
```

### Étape 1 — Créer la branche

```bash
git checkout feature/order-notifications
git checkout -b feature/order-authorization
```

### Étape 2 — DTO statut

**OrderStatusUpdateRequest.java** : `src/main/java/com/edgareldy/springboottutorial/dto/order/OrderStatusUpdateRequest.java`

```java
public record OrderStatusUpdateRequest(
    @JsonProperty("status")
    @NotNull(message = "Status must not be null")
    OrderStatus status
) {}
```

### Étape 3 — Repository — requêtes bornées par utilisateur

Dans `OrderRepository`, ajouter :

```java
// ROLE_USER : liste paginée bornée par userId
@EntityGraph(attributePaths = {"items", "items.product"})
Page<Order> findByUserId(Long userId, Pageable pageable);

// ROLE_USER : détail borné par userId (retourne Optional.empty() si pas propriétaire)
@Query("SELECT o FROM Order o JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :id AND o.user.id = :userId")
Optional<Order> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
```

### Étape 4 — Service — logique d'autorisation

Modifier `OrderService` :

```java
// ROLE_USER : ses commandes seulement. ROLE_ADMIN : toutes les commandes.
PageResponse<OrderResponse> findAll(String username, boolean isAdmin, Pageable pageable);

// ROLE_USER : 404 si pas propriétaire. ROLE_ADMIN : toujours accessible.
OrderResponse findById(Long id, String username, boolean isAdmin);

// ROLE_ADMIN uniquement
OrderResponse updateStatus(Long id, OrderStatus status);
```

Implémenter dans `OrderServiceImpl` :
- `findAll` : si `isAdmin` → `orderRepository.findAll(pageable)` ; sinon → `findByUserId(userId, pageable)`
- `findById` : si `isAdmin` → `findByIdWithDetails(id)` ; sinon → `findByIdAndUserId(id, userId)` → 404 si empty (ne pas révéler l'existence d'une commande d'un autre utilisateur)
- `updateStatus` : charger la commande, vérifier transition valide, sauvegarder
  - Transitions valides : `PENDING → CONFIRMED`, `PENDING → CANCELLED`, `CONFIRMED → CANCELLED`
  - Transition invalide → `BusinessRuleException` (422)

### Étape 5 — Contrôleur

Réécrire `OrderController` :

```java
@GetMapping
public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> findAll(
        @AuthenticationPrincipal UserDetails userDetails,
        Pageable pageable) {
    boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    return ResponseEntity.ok(ApiResponse.success(
            orderService.findAll(userDetails.getUsername(), isAdmin, pageable),
            "Orders retrieved successfully"));
}

@GetMapping("/{id}")
public ResponseEntity<ApiResponse<OrderResponse>> findById(
        @PathVariable Long id,
        @AuthenticationPrincipal UserDetails userDetails) {
    boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    return ResponseEntity.ok(ApiResponse.success(
            orderService.findById(id, userDetails.getUsername(), isAdmin),
            "Order retrieved successfully"));
}

@PatchMapping("/{id}/status")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody OrderStatusUpdateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(
            orderService.updateStatus(id, request.status()),
            "Order status updated successfully"));
}
```

Supprimer `PUT /api/v1/orders/{id}` (ancien endpoint de mise à jour générale),
ou le garder mais le réserver à ROLE_ADMIN.

### Étape 6 — Sécurité dans SecurityConfig

```java
.requestMatchers(HttpMethod.GET, "/api/v1/orders", "/api/v1/orders/**")
    .hasAnyRole("USER", "ADMIN")
.requestMatchers(HttpMethod.POST, "/api/v1/orders")
    .hasRole("USER")
.requestMatchers(HttpMethod.PATCH, "/api/v1/orders/*/status")
    .hasRole("ADMIN")
```

Vérifier également que `GET /api/v1/cart/**` reste protégé par `hasRole("USER")`.

### Étape 7 — Tests d'autorisation (Testcontainers)

**OrderAuthorizationTest** : `@SpringBootTest` + Testcontainers

Scénario avec 3 utilisateurs (créés en DB via service) :
- `userA` : ROLE_USER
- `userB` : ROLE_USER
- `admin` : ROLE_ADMIN

Tests :
- `_01_UserA_CanSeeOnlyOwnOrders` : GET /orders → contient uniquement commandes de userA
- `_02_UserA_CannotSeeUserB_Order` : GET /orders/{idCommande_userB} → 404 (pas 403, ne pas révéler l'existence)
- `_03_Admin_CanSeeAllOrders` : GET /orders → contient commandes de tous les utilisateurs
- `_04_Admin_CanSeeAnyOrderById` : GET /orders/{idCommande_userA} → 200
- `_05_UserA_CannotUpdateStatus` : PATCH /orders/{id}/status → 403
- `_06_Admin_CanUpdateStatus_ValidTransition` : PENDING → CONFIRMED → 200
- `_07_Admin_CannotUpdateStatus_InvalidTransition` : CONFIRMED → PENDING → 422
- `_08_UnauthenticatedUser_CannotAccessOrders` : GET /orders → 401

### Étape 8 — README

Ajouter section "Authorization" :
- Comportement par rôle sur chaque endpoint
- Transitions de statut autorisées
- Règle 404 vs 403 pour les commandes d'autres utilisateurs


## Validation finale

### Agents a invoquer dans cet ordre

#### 1. Agent test-runner -- verification des tests

Invoquer l'agent test-runner apres avoir termine tous les commits d'implementation.
L'agent lance la suite complete et renvoie un rapport des echecs avec leur cause probable.
Pour chaque echec : corriger le code ou le test dans un commit atomique dedie, puis relancer
l'agent jusqu'a ce que la suite soit entierement verte.

Note : si Testcontainers echoue, verifier docker-desktop et relancer avec
DOCKER_HOST=unix:///home/edgareldy/.docker/desktop/docker.sock ./mvnw test

#### 2. Agent code-reviewer -- relecture du code produit

Une fois les tests verts, invoquer l'agent code-reviewer. L'agent verifie :
pattern contrat/implementation, ApiResponse<T>, @Transactional, absence de fuite d'entite
JPA, presence de tests pour chaque methode publique.
Corriger chaque ecart bloquant dans un commit atomique dedie avant de clore la branche.
Les ecarts mineurs peuvent etre traites ou documentes.

#### 3. Agent spring-boot-expert -- conseil technique (a la demande)

Invoquer l'agent spring-boot-expert a tout moment en cas de doute sur une approche
technique : choix de pattern, comportement du framework, risque de N+1, gestion des
transactions, etc. A consulter avant d'implementer une decision incertaine.
## Commits attendus (atomiques)

```
feat(dto): add OrderStatusUpdateRequest DTO
refactor(repository): add findByUserId and findByIdAndUserId to OrderRepository
refactor(service): update OrderService interface with role-aware methods
refactor(service): implement authorization logic in OrderServiceImpl
feat(controller): rewrite OrderController with role-based access and status update endpoint
refactor(security): configure order and cart authorization rules in SecurityConfig
test(integration): add OrderAuthorizationTest with two users and admin
docs(readme): add authorization section with role matrix
```

## Règles impératives

- Un ROLE_USER voit toujours 404 (jamais 403) pour une commande qui n'est pas la sienne
- Pas de préfixe /admin dans les routes
- Contrôles de propriété dans le SERVICE/REPOSITORY, pas uniquement dans le controller
- Javadoc anglais sur chaque nouveau type
- Un commit par fichier
- Lancer les tests (DOCKER_HOST=unix:///home/edgareldy/.docker/desktop/docker.sock ./mvnw test) et corriger tous les echecs avant de continuer
- Relire le code une fois les tests verts et corriger les findings bloquants avant de clore la branche
- Mettre a jour le README (section de la branche courante) en dernier commit
- Ne pas pousser
