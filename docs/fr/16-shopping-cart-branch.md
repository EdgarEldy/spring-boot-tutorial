# Branche : feature/shopping-cart

Cette branche introduit un panier d'achat lié à l'utilisateur authentifié. Elle illustre la
création lazy d'entité, le contrôle de propriété et un pattern many-to-many entre utilisateurs
et produits sans entité de jointure dédiée.

---

## Ce que vous allez apprendre

- Comment créer une ressource de façon lazy à la première utilisation plutôt qu'à la création explicite
- Comment enforcer la propriété dans la couche service (`@AuthenticationPrincipal` transmis au service)
- Comment charger une association bidirectionnelle eagerly avec `@EntityGraph` sur une requête de repository
- En quoi `@WithMockUser` diffère de `.with(user(...))` dans les slices `@WebMvcTest` avec filtres désactivés
- Pourquoi `CartMapper.toResponse` doit être une méthode `default` quand la cible est un `record`

---

## Ce que cette branche apporte

### Migration Flyway

| Migration | Ce qu'elle fait |
|---|---|
| V17 | Crée les tables `carts` (une ligne par utilisateur) et `cart_items` (produit, quantité, une ligne par produit par panier) |

V17 est utilisée à la place de V16 car V16 est réservée par `feature/product-images` sur une
branche parallèle. Choisir V17 en avance évite un conflit de checksum Flyway lors de la fusion.

### Entités Cart / CartItem

`Cart` a `@OneToOne AppUser user` et `@OneToMany(cascade = ALL, orphanRemoval = true) List<CartItem> items`.
`CartItem` a `@ManyToOne Cart cart`, `@ManyToOne Product product`, `int quantity` et un
`@Transient BigDecimal subtotal` calculé à la lecture.

### CartService / CartServiceImpl

`CartServiceImpl` implémente les règles suivantes :

- **Création lazy :** une ligne de panier n'est créée qu'au premier appel à `addItem`. `getCart`
  ne crée jamais de panier ; il retourne une réponse vide si aucun n'existe. Cela évite les
  lignes orphelines pour les utilisateurs qui parcourent le catalogue sans rien ajouter.
- **Vérification produit actif :** les produits avec `active = false` ne peuvent pas être ajoutés.
- **Incrément sur doublon :** ajouter un article déjà dans le panier incrémente sa quantité
  plutôt que d'insérer une ligne en doublon.
- **Contrôle de propriété :** `updateItem` et `removeItem` vérifient que l'article appartient
  au panier de l'utilisateur authentifié avant toute modification.

### CartController

Cinq endpoints, tous sécurisés avec `hasRole("USER")` :

| Méthode | URL | Description |
|---|---|---|
| GET | `/api/v1/cart` | Voir le panier courant |
| POST | `/api/v1/cart/items` | Ajouter un produit au panier |
| PATCH | `/api/v1/cart/items/{itemId}` | Modifier la quantité d'un article |
| DELETE | `/api/v1/cart/items/{itemId}` | Retirer un article |
| DELETE | `/api/v1/cart` | Vider le panier |

L'utilisateur authentifié est résolu avec `@AuthenticationPrincipal` et transmis au service,
jamais lu depuis le corps de la requête.

---

## Décisions techniques

**`@EntityGraph` sur `CartRepository.findByUserId`**
`Cart.items` et `CartItem.product` sont lazy par défaut. Les charger à l'intérieur d'une
méthode de service après le `save` initial déclencherait des sélects supplémentaires hors de
la transaction d'origine. `findByUserId` avec
`@EntityGraph(attributePaths = {"items", "items.product"})` charge le graphe complet en une
seule jointure, et est appelé après chaque mutation pour retourner l'état persisté.

**`CartMapper.toResponse` comme méthode `default`**
MapStruct `@AfterMapping` ne peut pas modifier un `record` déjà construit. `toResponse` est
une méthode `default` qui itère les articles, calcule les sous-totaux, somme le total du
panier et construit le record manuellement. Même pattern que `ProductMapper.toDetailResponse`.

**`@WithMockUser` plutôt que `.with(user(...))` dans `CartControllerTest`**
Avec `@AutoConfigureMockMvc(addFilters = false)`, `SecurityContextHolderFilter` ne tourne
jamais, donc `.with(user(...))` ne peuple pas `SecurityContextHolder` et
`@AuthenticationPrincipal` retourne `null`. `@WithMockUser` est traité par
`WithSecurityContextTestExecutionListener`, qui peuple le contexte avant l'exécution de la
méthode de test, contournant entièrement le filtre.

---

## Fichiers clés

| Fichier | Rôle |
|---|---|
| `src/main/java/.../entity/Cart.java` | Entité JPA, cascade + orphanRemoval |
| `src/main/java/.../entity/CartItem.java` | Entité JPA avec sous-total transient |
| `src/main/java/.../dto/cart/CartItemRequest.java` | DTO de requête ajout d'article |
| `src/main/java/.../dto/cart/CartItemUpdateRequest.java` | DTO de modification de quantité |
| `src/main/java/.../dto/cart/CartItemResponse.java` | DTO de réponse par article |
| `src/main/java/.../dto/cart/CartResponse.java` | DTO de réponse panier complet |
| `src/main/java/.../mapper/CartMapper.java` | default toResponse avec calcul des sous-totaux |
| `src/main/java/.../repository/CartRepository.java` | Chargement eager EntityGraph |
| `src/main/java/.../repository/CartItemRepository.java` | Requêtes par article |
| `src/main/java/.../service/CartService.java` | Contrat de service panier |
| `src/main/java/.../service/impl/CartServiceImpl.java` | Création lazy, propriété, incrément |
| `src/main/java/.../controller/CartController.java` | 5 endpoints, @AuthenticationPrincipal |
| `src/main/resources/db/migration/V17__add_cart_tables.sql` | Migration de schéma |
