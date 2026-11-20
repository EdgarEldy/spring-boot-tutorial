# Branche : feature/order-checkout

Cette branche remplace l'endpoint de création manuelle de commande par une opération de
checkout atomique qui convertit le panier de l'utilisateur authentifié en commande. Elle
illustre l'intégrité transactionnelle, la re-lecture des prix comme mesure de sécurité, et
l'utilisation de `orphanRemoval` JPA pour vider une collection enfant proprement au sein
d'une transaction gérée.

---

## Ce que vous allez apprendre

- Comment implémenter une transaction "convertir panier en commande" atomique dans un service Spring
- Pourquoi les prix doivent être re-lus depuis la base de données au checkout, et non trust depuis le panier
- Comment vider une collection JPA avec `orphanRemoval` correctement vs un appel bulk-delete de repository
- Comment lier une commande à l'utilisateur authentifié via `@AuthenticationPrincipal` sans mettre l'id dans le corps de la requête

---

## Ce que cette branche apporte

### Migration Flyway

| Migration | Ce qu'elle fait |
|---|---|
| V18 | Ajoute une colonne `user_id` nullable sur `orders` avec une FK vers `app_users`. Nullable car les commandes CRUD legacy (avant le checkout) n'ont pas d'utilisateur. |

### Mise à jour de l'entité Order

`Order` gagne `@ManyToOne(fetch = LAZY) AppUser user`. La colonne est nullable ; elle est
définie uniquement pour les commandes checkout, jamais pour les commandes CRUD legacy.

### Flux de checkout

`OrderServiceImpl.checkout(String username)` :

1. Charge le panier de l'utilisateur authentifié (lève une 404 si le panier est vide).
2. Pour chaque `CartItem`, re-lit le produit depuis la base de données via `productRepository.findByIdWithCategory` pour obtenir le prix actuel. Cela protège contre la manipulation de prix via un panier obsolète.
3. Crée un `OrderItem` par article du panier en utilisant le snapshot produit (nom + prix unitaire courant).
4. Sauvegarde la `Order` avec tous ses articles en un seul appel `@Transactional`.
5. Vide le panier en appelant `cart.getItems().clear()`. Parce que `Cart.items` a `orphanRemoval = true`, Hibernate supprime les lignes `CartItem` orphelines au flush, dans la même transaction. Voir les décisions techniques.

### Changement dans OrderController

`create(@RequestBody OrderRequest)` est remplacé par `checkout(@AuthenticationPrincipal UserDetails)`.
L'endpoint est `POST /api/v1/orders` sans corps de requête. L'identité de l'utilisateur
provient du JWT, pas du client.

`SecurityConfig` est mis à jour : `POST /api/v1/orders` passe de `hasRole("ADMIN")` à
`hasRole("USER")`.

---

## Décisions techniques

**Re-lecture des prix depuis la base de données au checkout**
Un `CartItem` stocke la quantité et une référence à un `Product`. Il ne stocke pas le prix.
Au checkout, le produit est rechargé avec `findByIdWithCategory` pour obtenir l'`unitPrice`
actuel depuis la base de données. Cela empêche un client de manipuler les prix en modifiant
un article de panier ajouté quand le prix était plus bas. Le test
`_checkout_06_ShouldUsePriceFromDB_NotFromCachedCartItem` couvre cet invariant.

**`cart.getItems().clear()` plutôt que `cartItemRepository.deleteAllByCartId`**
Dans une méthode `@Transactional` où l'entité `Cart` est gérée par Hibernate, appeler
`cartItemRepository.deleteAllByCartId()` émet un statement `DELETE` SQL. Cependant, comme
`Cart` est toujours géré avec sa collection `items` en mémoire, le cascade `PERSIST` de
Hibernate réinsère les articles juste supprimés au flush. L'approche correcte est
`cart.getItems().clear()` : Hibernate détecte les orphelins et planifie leur suppression via
`orphanRemoval`, dans le même cycle de flush, sans la réinsertion. Ce bug a été découvert et
corrigé dans `feature/integration`.

---

## Fichiers clés

| Fichier | Rôle |
|---|---|
| `src/main/java/.../service/OrderService.java` | Ajoute `checkout(String username)` |
| `src/main/java/.../service/impl/OrderServiceImpl.java` | Implémentation checkout atomique |
| `src/main/java/.../controller/OrderController.java` | Remplace create par checkout |
| `src/main/resources/db/migration/V18__add_user_id_to_orders.sql` | FK user nullable |
