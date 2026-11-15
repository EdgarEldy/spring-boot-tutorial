# Branche : feature/orders

Cette branche ajoute la ressource Order et introduit le système d'événements applicatifs de
Spring. Une commande relie un client à un produit ; sa création est l'opération d'écriture
la plus complexe de l'application car elle touche deux autres ressources et publie un
événement de domaine.

---

## Ce que vous allez apprendre

- Comment publier un événement de domaine depuis l'intérieur d'une transaction avec `ApplicationEventPublisher`
- Pourquoi `@TransactionalEventListener(phase = AFTER_COMMIT)` est plus sûr que `@EventListener` pour les effets de bord post-commit
- Comment éviter les requêtes N+1 sur les endpoints de liste avec des projections JPQL (`SELECT new ClassName(...)`)
- Comment charger un graphe d'objets complet pour les endpoints de détail avec `JOIN FETCH`
- Comment construire un endpoint REST imbriqué (`/customers/{id}/orders`)
- Comment ajouter des gardes de suppression sur plusieurs ressources liées

---

## Ce que cette branche apporte

### Entité Order

`Order` a une relation `@ManyToOne` vers `Customer` et `Product`, les deux `NOT NULL`. Elle
stocke également `quantity` et `totalPrice` (dénormalisé au moment de la création).

### OrderCreatedEvent et OrderCreatedEventListener

`OrderServiceImpl.create()` est annoté `@Transactional`. Après avoir sauvegardé la commande,
il appelle `applicationEventPublisher.publishEvent(new OrderCreatedEvent(savedOrder))`.

`OrderCreatedEventListener` gère l'événement avec
`@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.

> **Pourquoi AFTER_COMMIT ?** `@EventListener` s'exécute de façon synchrone dans la
> transaction courante. Si le listener lève une exception ou si la transaction est annulée
> après l'exécution du listener, l'événement a déjà été traité pour une ligne qui n'existe
> plus. `AFTER_COMMIT` garantit que le listener ne s'exécute qu'après que la ligne de base
> de données est durable. Si la transaction est annulée, le listener ne s'exécute pas.

### Projection JPQL pour l'endpoint de liste

`OrderRepository.findAllProjected(Pageable)` utilise une requête JPQL `SELECT new` qui
sélectionne exactement les colonnes nécessaires pour `OrderProjection`. Cela charge toutes
les données d'une page complète de commandes en une seule requête SQL, sans select
supplémentaire pour le client ou le produit lié.

### JOIN FETCH pour l'endpoint de détail

`OrderRepository.findByIdWithDetails(Long id)` utilise `JOIN FETCH` pour charger le client
et le produit en une seule requête pour la vue de détail. C'est approprié pour un lookup
mono-ligne où le graphe d'objets complet est toujours nécessaire.

### Gardes de suppression

`CustomerServiceImpl.delete()` et `ProductServiceImpl.delete()` vérifient tous deux
`OrderRepository.existsByCustomerId(id)` et `OrderRepository.existsByProductId(id)` avant
de supprimer l'entité. Cela empêche les violations de clé étrangère sur
`orders.customer_id` et `orders.product_id` de remonter au client sous forme d'erreurs 500.

### Endpoint imbriqué

`CustomerController` expose `GET /api/v1/customers/{id}/orders`. Il vérifie d'abord que le
client existe (en appelant `CustomerService.findById`, levant 404 si absent), puis délègue à
`OrderService.findAll` filtré par l'id du client. La logique reste au niveau du contrôleur
car elle orchestre deux services ; aucune méthode de service dédiée n'a été ajoutée.

---

## Fichiers clés

| Fichier | Rôle |
|---|---|
| `src/main/java/.../entity/Order.java` | Entité JPA Order |
| `src/main/java/.../event/OrderCreatedEvent.java` | Record d'événement de domaine |
| `src/main/java/.../event/OrderCreatedEventListener.java` | Listener AFTER_COMMIT |
| `src/main/java/.../repository/OrderRepository.java` | Requêtes projection + JOIN FETCH |
| `src/main/java/.../service/OrderService.java` | Interface de service |
| `src/main/java/.../service/impl/OrderServiceImpl.java` | @Transactional, publie l'événement |
| `src/main/java/.../controller/OrderController.java` | Endpoints REST |
| `src/main/java/.../controller/CustomerController.java` | Endpoint imbriqué /orders |
