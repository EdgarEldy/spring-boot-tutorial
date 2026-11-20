# Branche : feature/order-notifications

Cette branche ajoute des notifications email asynchrones pour les commandes checkout. Elle
montre comment câbler `@Async` sur un executor nommé, comment recharger un agrégat complet
après le commit de la transaction, et pourquoi les échecs d'email ne doivent jamais se
propager à l'appelant.

---

## Ce que vous allez apprendre

- Comment envoyer des emails avec `JavaMailSender` et `SimpleMailMessage`
- Pourquoi la logique email doit tourner sur un pool de threads dédié (`@Async`) pour ne pas bloquer le thread HTTP
- Pourquoi les exceptions mail doivent être catchées et loggées, jamais relancées
- Comment recharger un graphe d'entité complet après `AFTER_COMMIT` pour avoir l'utilisateur et les articles en contexte
- Comment tester des listeners async de façon synchrone en stubbant les dépendances plutôt que l'executor

---

## Ce que cette branche apporte

### NotificationProperties

`NotificationProperties` est un record `@ConfigurationProperties(prefix = "app.notifications")`
avec un seul champ `adminEmail`. Il est enregistré dans `AsyncConfig` aux côtés du bean
`emailTaskExecutor`.

### OrderEmailService / OrderEmailServiceImpl

`OrderEmailService` définit deux méthodes : `sendOrderConfirmation(Order, AppUser)` pour le
client et `sendAdminNotification(Order)` pour l'administrateur.

`OrderEmailServiceImpl` est annoté avec `@Async("emailTaskExecutor")`. Chaque méthode :

- Construit un `SimpleMailMessage` avec un contenu texte brut
- Appelle `javaMailSender.send(message)`
- Enveloppe l'appel dans un `try/catch(MailException)` et logue l'exception au niveau ERROR
  si l'envoi échoue. L'exception n'est jamais relancée. Un echec d'email ne doit pas rollbacker
  la commande déjà committée.

### Extension de OrderRepository

`findByIdWithItemsAndUser` est ajoutée avec un JPQL `LEFT JOIN FETCH` sur `items` et `user`.
Une nouvelle méthode est ajoutée plutôt que de modifier `findByIdWithDetails` car la méthode
existante utilise `INNER JOIN` sur `customer` et `product` (commandes CRUD legacy), ce qui
retournerait `Optional.empty()` pour les commandes checkout qui n'ont pas de `customer_id` ni
de `product_id`.

### OrderCreatedEventListener

`OrderCreatedEventListener` est enrichi pour :

1. Tourner sur `emailTaskExecutor` via `@Async("emailTaskExecutor")`.
2. Recharger la commande complète depuis la base de données avec `findByIdWithItemsAndUser`. L'event ne transporte que l'id de commande ; l'objet complet n'est pas passé dans l'event pour éviter les problèmes de données obsolètes après le commit de la transaction.
3. Appeler `orderEmailService.sendOrderConfirmation` si la commande a un utilisateur, et `orderEmailService.sendAdminNotification` inconditionnellement.

---

## Décisions techniques

**`findByIdWithItemsAndUser` séparé plutôt que modifier `findByIdWithDetails`**
`findByIdWithDetails` utilise `INNER JOIN` sur `customer` et `product`. Les commandes checkout
ont `customer_id = NULL` et `product_id = NULL`, donc un `INNER JOIN` sur ces colonnes ne
retourne aucune ligne. Modifier la méthode existante casserait `OrderServiceImpl.findById` et
tous ses tests. Ajouter une nouvelle méthode garde les deux chemins de lecture indépendants.

**`@Async` sur le listener d'evenement**
`@TransactionalEventListener(phase = AFTER_COMMIT)` garantit déjà que le listener tourne
après le commit de la transaction de commande. Ajouter `@Async("emailTaskExecutor")` déplace
l'exécution hors du thread de requête HTTP, de sorte que la réponse client (201 Created)
n'est pas retardée par la latence SMTP.

**Stubbing par méthode de test, pas dans `@BeforeEach`**
Le mode strict de Mockito (`STRICT_STUBS`) lève `UnnecessaryStubbingException` pour les
stubs configurés mais jamais consommés dans un test donné. `notificationProperties.adminEmail()`
est stubbé uniquement dans les méthodes de test qui l'invoquent réellement, pas dans `@BeforeEach`.

---

## Fichiers clés

| Fichier | Rôle |
|---|---|
| `src/main/java/.../config/NotificationProperties.java` | `@ConfigurationProperties(prefix="app.notifications")` |
| `src/main/java/.../service/OrderEmailService.java` | Contrat de service email |
| `src/main/java/.../service/impl/OrderEmailServiceImpl.java` | Async, email texte brut, exception avalée |
| `src/main/java/.../event/OrderCreatedEventListener.java` | Recharge la commande, dispatche les emails |
| `src/main/java/.../repository/OrderRepository.java` | Ajoute findByIdWithItemsAndUser |
