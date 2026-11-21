# Phase 6 — feature/order-notifications (emails après commande)

Crée la branche `feature/order-notifications` depuis `feature/order-checkout`.
Implémente les emails client et admin envoyés après commit de la commande.

## Prérequis

- Phase 5 terminée (`feature/order-checkout` complète)

## Instructions pour l'agent

### Étape 0 — Vérification Git

```bash
git log --oneline feature/order-checkout -1
git branch | grep order-notifications
```

### Étape 1 — Créer la branche

```bash
git checkout feature/order-checkout
git checkout -b feature/order-notifications
```

### Étape 2 — Configuration

**NotificationProperties.java** : `src/main/java/com/edgareldy/springboottutorial/config/NotificationProperties.java`

```java
@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(
    @DefaultValue("admin@example.com") String adminEmail
) {}
```

Enregistrer via `@EnableConfigurationProperties(NotificationProperties.class)`.

Ajouter dans `application.yml` :
```yaml
app:
  notifications:
    admin-email: ${APP_NOTIFICATION_ADMIN_EMAIL:admin@example.com}
```

Ajouter dans `.env.example` : `APP_NOTIFICATION_ADMIN_EMAIL=admin@example.com`

### Étape 3 — Service email

**OrderEmailService interface** :
```java
public interface OrderEmailService {
    void sendOrderConfirmation(AppUser user, Order order);
    void sendAdminNotification(Order order);
}
```

**OrderEmailServiceImpl** :
- `@Async("emailTaskExecutor")` sur les deux méthodes (réutiliser l'executor déjà configuré dans le projet)
- Destinataire client : `user.getEmail()`
- Destinataire admin : `notificationProperties.adminEmail()`
- Sujet client : "Order Confirmation - " + order.getOrderNumber()
- Sujet admin : "New Order Received - " + order.getOrderNumber()
- Corps : texte simple avec numéro de commande, lignes, quantités, prix unitaires et total
- En cas d'erreur SMTP : `log.error(...)` UNIQUEMENT — ne pas relancer l'exception, ne pas marquer la commande comme échouée

Format du corps (texte simple) :
```
Order: {orderNumber}
Date: {createdAt}

Items:
- {productName} x{quantity} @ {unitPrice} = {subtotal}
...

Total: {totalAmount}

Thank you for your order.
```

### Étape 4 — Enrichir OrderCreatedEventListener

Dans `OrderCreatedEventListener.onOrderCreated` (déjà annoté `@TransactionalEventListener(AFTER_COMMIT)`) :

```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onOrderCreated(OrderCreatedEvent event) {
    log.info("Order {} created, sending notifications", event.orderNumber());
    // Recharger Order avec items et user depuis DB (l'event ne les porte pas directement)
    Order order = orderRepository.findByIdWithDetails(event.orderId())
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + event.orderId()));
    AppUser user = order.getUser();
    if (user != null) {
        orderEmailService.sendOrderConfirmation(user, order);
    }
    orderEmailService.sendAdminNotification(order);
}
```

**Important :** le listener s'exécute APRÈS commit. Les erreurs SMTP ne peuvent plus annuler la commande.

### Étape 5 — Enrichir OrderCreatedEvent

Vérifier que `OrderCreatedEvent` porte `orderId` et `orderNumber`.
Ajouter `orderNumber` si absent (pour le logging dans le listener).

### Étape 6 — Tests

**OrderEmailServiceImplTest** :
- `_01_ShouldSendConfirmationToUser_WhenOrderCreated` : vérifier `JavaMailSender.send()` appelé, destinataire = user email, sujet contient order number
- `_02_ShouldSendNotificationToAdmin_WhenOrderCreated` : destinataire = adminEmail configuré
- `_03_ShouldNotPropagateSmtpException_WhenMailFails` : `JavaMailSender.send()` lève `MailSendException`, vérifier que la méthode retourne sans exception

**OrderCreatedEventListenerTest** :
- `_01_ShouldCallEmailService_AfterCommit` : vérifier que `orderEmailService.sendOrderConfirmation` et `sendAdminNotification` sont appelés

**Note sur les tests :** utiliser `@MockitoBean JavaMailSender` dans les tests `@SpringBootTest`.
Le profil `test` utilise déjà `@ActiveProfiles("test")` — vérifier que `spring.mail` est configuré
dans `application-test.yml` ou mocker `JavaMailSender`.

### Étape 7 — README

Ajouter section "Email Notifications" :
- Configuration `APP_NOTIFICATION_ADMIN_EMAIL`
- Configuration MailHog en dev
- Comportement en cas d'erreur SMTP


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
feat(config): add NotificationProperties and admin-email configuration
feat(service): add OrderEmailService interface
feat(service): implement OrderEmailServiceImpl with SMTP error handling
refactor(event): add orderNumber to OrderCreatedEvent if missing
refactor(event): extend OrderCreatedEventListener to send emails after commit
test(service): add OrderEmailServiceImplTest
test(event): add OrderCreatedEventListenerTest
docs(readme): add email notifications section with MailHog configuration
```

## Règles impératives

- Erreur SMTP = log uniquement, jamais de propagation
- Emails envoyés APRÈS commit (AFTER_COMMIT), jamais pendant la transaction
- Ne pas coder d'adresse email en dur
- Javadoc anglais sur chaque nouveau type
- Un commit par fichier
- Lancer les tests (DOCKER_HOST=unix:///home/edgareldy/.docker/desktop/docker.sock ./mvnw test) et corriger tous les echecs avant de continuer
- Relire le code une fois les tests verts et corriger les findings bloquants avant de clore la branche
- Mettre a jour le README (section de la branche courante) en dernier commit
- Ne pas pousser
