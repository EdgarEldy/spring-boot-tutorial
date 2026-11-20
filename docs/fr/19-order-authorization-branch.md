# Branche : feature/order-authorization

Cette branche rend la ressource commande consciente des rôles : les utilisateurs normaux ne
peuvent voir et agir que sur leurs propres commandes, tandis que les admins ont une visibilité
complète. Elle ajoute également un système de transitions de statut avec matrice de cycle de
vie et le couvre avec un vrai test Testcontainers end-to-end.

---

## Ce que vous allez apprendre

- Comment implémenter des endpoints de liste et de détail conscients du rôle dans la couche service
- Comment enforcer une matrice de transition de statut et retourner 422 pour les transitions invalides
- Comment combiner `@PreAuthorize` et `@AuthenticationPrincipal` sur une méthode de contrôleur
- Comment écrire un test end-to-end multi-acteurs avec Testcontainers (userA, userB, admin)
- Pourquoi `INNER JOIN` sur des colonnes FK nullable supprime silencieusement des lignes, et quand utiliser `LEFT JOIN FETCH`

---

## Ce que cette branche apporte

### Accès aux commandes conscient du rôle

`OrderService` gagne deux nouvelles signatures :

- `findAll(String username, boolean isAdmin, Pageable)` - les admins reçoivent toutes les
  commandes ; les utilisateurs normaux reçoivent uniquement les commandes où
  `order.user.username = username` (via la FK `user_id`).
- `findById(Long id, String username, boolean isAdmin)` - les admins peuvent récupérer
  n'importe quelle commande ; un utilisateur normal reçoit un 404 si la commande appartient
  à un autre utilisateur.

### Cycle de vie du statut de commande

`updateStatus(Long id, OrderStatus newStatus)` enforece la matrice de transition suivante :

| De | Transitions autorisées |
|---|---|
| PENDING | CONFIRMED, CANCELLED |
| CONFIRMED | CANCELLED |
| CANCELLED | (aucune) |

Toute autre transition retourne 422 `Unprocessable Entity`.

### Réécriture de OrderController

`OrderController` est réécrit avec `@AuthenticationPrincipal` et `@PreAuthorize` :

- `GET /api/v1/orders` - `@PreAuthorize("isAuthenticated()")`, délègue avec username + isAdmin
- `GET /api/v1/orders/{id}` - `@PreAuthorize("isAuthenticated()")`, délègue avec username + isAdmin
- `PATCH /api/v1/orders/{id}/status` - `@PreAuthorize("hasRole('ADMIN')")`, corps `OrderStatusUpdateRequest`

### Correction de bug : LEFT JOIN FETCH sur FK nullable

`findByIdWithDetails` utilisait précédemment `INNER JOIN` sur `customer` et `product`. Les
commandes checkout ont `customer_id = NULL` et `product_id = NULL`. Un `INNER JOIN` sur une
colonne nullable ne retourne silencieusement aucune ligne quand la valeur est null, donc les
commandes checkout étaient invisibles pour `findById`. Corrigé en basculant vers `LEFT JOIN FETCH`
sur ces deux associations.

### OrderAuthorizationTest

8 cas de test d'intégration Testcontainers couvrant :

- userA peut voir sa propre commande
- userA reçoit 404 sur la commande de userB
- admin peut voir toutes les commandes (findAll et findById)
- admin peut mettre à jour le statut (PENDING vers CONFIRMED)
- un utilisateur normal ne peut pas mettre à jour le statut (403)
- une transition de statut invalide retourne 422

---

## Décisions techniques

**404 plutôt que 403 pour l'accès croisé aux commandes**
Retourner 403 (Forbidden) révélerait qu'une commande avec l'id donné existe, ce qui est en
soi une information. Retourner 404 (Not Found) pour un utilisateur essayant d'accéder à la
commande d'un autre utilisateur empêche l'enumération des ids de commandes d'autres utilisateurs.

**`findAllByCustomerId` ajouté à `OrderService`**
`CustomerController.findOrders` appelait précédemment `orderService.findAll` directement.
Avec la nouvelle signature consciente du rôle, `findAllByCustomerId(Long, Pageable)` est
ajoutée comme méthode dédiée pour l'endpoint imbriqué, découplée du `findAll` scopé sur l'utilisateur.

---

## Fichiers clés

| Fichier | Rôle |
|---|---|
| `src/main/java/.../dto/order/OrderStatusUpdateRequest.java` | DTO de requête PATCH statut |
| `src/main/java/.../service/OrderService.java` | findAll/findById conscients du rôle + updateStatus |
| `src/main/java/.../service/impl/OrderServiceImpl.java` | Matrice de transition, vérification propriété |
| `src/main/java/.../controller/OrderController.java` | @PreAuthorize + @AuthenticationPrincipal |
| `src/test/java/.../OrderAuthorizationTest.java` | Test e2e Testcontainers 8 cas |
