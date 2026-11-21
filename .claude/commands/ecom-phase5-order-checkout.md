# Phase 5 — feature/order-checkout (validation panier → commande)

Crée la branche `feature/order-checkout` depuis `feature/shopping-cart`.
Implémente la conversion atomique du panier en commande multi-produits.

## Prérequis

- Phase 4 terminée (`feature/shopping-cart` complète)

## Instructions pour l'agent

### Étape 0 — Vérification Git

```bash
git log --oneline feature/shopping-cart -1   # noter SHA
git branch | grep order-checkout
```

### Étape 1 — Créer la branche

```bash
git checkout feature/shopping-cart
git checkout -b feature/order-checkout
```

### Étape 2 — DTO Checkout

**CheckoutRequest.java** : record vide (pas de userId, pas de prix, pas de total, pas de statut).

```java
/**
 * Request body for POST /api/v1/orders. Intentionally empty: the backend
 * reads the authenticated user's cart, re-fetches prices from the database,
 * and calculates all amounts server-side. The client never supplies
 * ownership, prices or totals.
 */
public record CheckoutRequest() {}
```

### Étape 3 — Méthode checkout dans OrderService

Ajouter à `OrderService` :
```java
OrderResponse checkout(String username);
```

### Étape 4 — Implémenter checkout dans OrderServiceImpl

Flux DANS UNE SEULE TRANSACTION (`@Transactional`) :

1. `AppUser user = appUserRepository.findByUsername(username).orElseThrow(...)`
2. `Cart cart = cartRepository.findByUserId(user.getId()).orElseThrow(...)` — 404 si pas de panier
3. Si `cart.getItems().isEmpty()` → lever `BusinessRuleException` (422) "Cart is empty"
4. Pour chaque `CartItem` :
   a. Recharger le produit depuis DB (`productRepository.findById(item.getProduct().getId())`)
   b. Si produit absent ou `product.isActive() == false` → lever `BusinessRuleException` (422)
   c. Calculer `subtotal = item.quantity * product.unitPrice` (BigDecimal, HALF_UP)
5. Générer `orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0,8).toUpperCase()`
6. Créer `Order` avec `user`, `status = PENDING`, `orderNumber`, sans `customer`/`product` mono
7. Créer un `OrderItem` par ligne du panier avec snapshot `productName`, `unitPrice`, `quantity`, `subtotal`
8. Calculer `totalAmount = somme des subtotals` (BigDecimal, HALF_UP)
9. Sauvegarder `Order` et ses `OrderItem`s
10. Vider le panier : `cartItemRepository.deleteAllByCartId(cart.getId())`
11. Publier `OrderCreatedEvent` (après save, avant return)
12. Retourner `OrderResponse`

**Important :** le vidage du panier doit être dans la MÊME transaction. Si la sauvegarde de la
commande échoue, le panier n'est PAS vidé (rollback automatique).

### Étape 5 — Modifier OrderController

Remplacer `create(@Valid @RequestBody OrderRequest request)` par :

```java
@PostMapping
public ResponseEntity<ApiResponse<OrderResponse>> checkout(
        @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                    orderService.checkout(userDetails.getUsername()),
                    "Order created successfully"));
}
```

**Ne plus accepter OrderRequest** pour la création. Les anciens endpoints PUT/DELETE restent
pour compatibilité CRUD mais POST crée maintenant depuis le panier.

### Étape 6 — CartItemRepository

Ajouter : `deleteAllByCartId(Long cartId)` (ou `deleteByCart(Cart cart)`)

### Étape 7 — Sécurité

Dans `SecurityConfig`, protéger `POST /api/v1/orders` avec `hasRole("USER")`.

### Étape 8 — Tests

**CheckoutServiceTest** (ou extension de OrderServiceImplTest) :
- `_checkout_01_ShouldCreateOrderFromCart_WhenCartIsValid` : 2 produits dans le panier, vérifier 2 OrderItems, totalAmount correct, panier vidé
- `_checkout_02_ShouldThrow422_WhenCartIsEmpty`
- `_checkout_03_ShouldThrow422_WhenProductIsInactive`
- `_checkout_04_ShouldNotClearCart_WhenOrderCreationFails` (simuler exception en DB)
- `_checkout_05_ShouldUsePriceFromDB_NotFromCart` : prix DB différent du prix mis en cache dans CartItem

**OrderControllerTest** :
- Mettre à jour `_checkout_01` : POST /api/v1/orders ne prend plus de body OrderRequest
- Vérifier `status = "PENDING"`, `order_number` présent, `items` non vide

### Étape 9 — README

Mettre à jour section Orders / ajouter section Checkout :
- Flux : panier → commande atomique
- Corps de la requête : vide
- Règles de sécurité (prix relus depuis DB)


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
feat(dto): add empty CheckoutRequest DTO
refactor(service): add checkout method to OrderService interface
feat(service): implement checkout in OrderServiceImpl with atomic cart-to-order conversion
refactor(controller): replace create endpoint with checkout from authenticated user's cart
refactor(repository): add deleteAllByCartId to CartItemRepository
refactor(security): protect POST /api/v1/orders with ROLE_USER
test(service): add checkout tests in OrderServiceImplTest
test(controller): update OrderControllerTest for checkout endpoint
docs(readme): add checkout flow section
```

## Règles impératives

- L'utilisateur est résolu depuis `@AuthenticationPrincipal`, jamais depuis un paramètre
- Le total est TOUJOURS calculé côté serveur depuis les prix DB
- Panier vidé dans la MÊME transaction
- Javadoc anglais sur chaque nouveau type
- Un commit par fichier
- Lancer les tests (DOCKER_HOST=unix:///home/edgareldy/.docker/desktop/docker.sock ./mvnw test) et corriger tous les echecs avant de continuer
- Relire le code une fois les tests verts et corriger les findings bloquants avant de clore la branche
- Mettre a jour le README (section de la branche courante) en dernier commit
- Ne pas pousser
