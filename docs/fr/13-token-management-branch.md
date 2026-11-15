# Branche : feature/token-management

Cette branche ajoute le cycle de vie complet des tokens qui avait été mis en place dans
`feature/auth` : activation par email, réinitialisation du mot de passe, blacklistage JWT
lors de la déconnexion et tokens de rafraîchissement. Elle introduit également trois patterns
d'infrastructure qui apparaissent fréquemment en production : l'envoi d'email asynchrone,
le nettoyage planifié et le verrouillage pessimiste pour les écritures concurrentes.

---

## Ce que vous allez apprendre

- Comment envoyer des emails de façon asynchrone avec `@Async` et un executor de tâches nommé
- Comment planifier un job de nettoyage récurrent avec `@Scheduled`
- Comment prévenir une condition de course sur un lookup de token unique avec `PESSIMISTIC_WRITE`
- Comment générer des tokens cryptographiquement imprévisibles avec `SecureRandom`
- Comment structurer plusieurs types de tokens comme des entités séparées avec des tables dédiées

---

## Ce que cette branche apporte

### Entités de tokens

| Entité | Table | Rôle |
|---|---|---|
| `ActivationToken` | `activation_tokens` | Vérification d'email après inscription |
| `PasswordResetToken` | `password_reset_tokens` | Flux de réinitialisation du mot de passe |
| `BlacklistedToken` | `blacklisted_tokens` | Tokens JWT révoqués (déconnexion) |
| `RefreshToken` | `refresh_tokens` | Token longue durée pour le renouvellement du access token |

Flyway V5 et V6 créent ces tables.

### EmailService

`EmailService` (interface) / `EmailServiceImpl` envoie des emails HTML via JavaMailSender.
Les méthodes sont annotées `@Async("emailTaskExecutor")`. L'executor nommé est configuré
dans `AsyncConfig` avec un pool de threads borné.

Les templates (`activate_account.html`, `reset_password.html`) sont des fichiers HTML
Thymeleaf dans `src/main/resources/templates/`. `MimeMessageHelper` construit le message
MIME avec substitution de variables inline.

> **Exigence @ActiveProfiles("test") :** `EmailServiceImpl` dépend de `JavaMailSender`. Dans
> le profil `test`, `spring.mail.host` n'est pas configuré, donc `JavaMailSender` n'est pas
> créé. Toute classe `@SpringBootTest` qui charge le contexte complet doit porter
> `@ActiveProfiles("test")`, sinon Spring échoue à trouver `JavaMailSender` et lève
> `BeanCreationException`. Les tests de contrôleurs et les tests unitaires ne sont pas
> affectés car ils mockent `EmailService`.

### SecureTokenGenerator

Génère des tokens URL-safe en utilisant `SecureRandom` et l'encodage Base64 URL. L'utilisation
de `SecureRandom` (et non de `Random` ou `UUID.randomUUID()`) garantit que le token est
cryptographiquement imprévisible et ne peut pas être deviné à partir de la séquence.

### TokenCleanupScheduler

`@Scheduled(cron = "0 0 * * * *")` s'exécute toutes les heures. Il supprime les tokens
d'activation expirés, les tokens de réinitialisation de mot de passe et les tokens blacklistés
dont la date d'expiration est passée. Garder des tokens périmés en base n'est pas un risque
de sécurité (un token expiré échoue à la validation), mais cela ralentit les lookups futurs.

### BlacklistedTokenService et PESSIMISTIC_WRITE

`BlacklistedTokenRepository.findByJti(String)` est annoté
`@Lock(LockModeType.PESSIMISTIC_WRITE)`. Lorsque deux requêtes de déconnexion concurrentes
arrivent avec le même token, les deux tentent de vérifier si le jti est déjà blacklisté.
Sans verrou de base de données, les deux lectures retournent "non trouvé" simultanément et
les deux procèdent à l'insertion, provoquant une violation de contrainte unique.
`PESSIMISTIC_WRITE` sérialise les deux requêtes au niveau de la base de données.

### Mises à jour de JwtAuthFilter

`JwtAuthFilter` extrait maintenant le claim `jti` du token et appelle
`blacklistedTokenService.isBlacklisted(jti)` avant de peupler le `SecurityContext`. L'appel
est enveloppé dans un `try/catch DataAccessException`. Si la base de données est indisponible,
le filtre logue l'erreur et laisse la requête passer plutôt que de casser l'authentification
pour tous les utilisateurs.

### Refactoring de AuthResponse

`AuthResponse` porte maintenant les champs `access_token`, `refresh_token` et `expires_in`
au lieu d'un seul `token`. Le champ `username` a été supprimé car le client peut décoder le
JWT pour récupérer l'email sans aller-retour supplémentaire.

---

## Fichiers clés

| Fichier | Rôle |
|---|---|
| `src/main/java/.../entity/user/ActivationToken.java` | Entité token d'activation |
| `src/main/java/.../entity/user/BlacklistedToken.java` | Entité JWT blacklisté |
| `src/main/java/.../entity/user/PasswordResetToken.java` | Entité token de reset |
| `src/main/java/.../entity/user/RefreshToken.java` | Entité token de rafraîchissement |
| `src/main/java/.../service/EmailService.java` | Interface email asynchrone |
| `src/main/java/.../service/impl/EmailServiceImpl.java` | Thymeleaf + @Async |
| `src/main/java/.../security/SecureTokenGenerator.java` | Génération de token cryptographique |
| `src/main/java/.../scheduler/TokenCleanupScheduler.java` | Nettoyage @Scheduled |
| `src/main/java/.../service/BlacklistedTokenService.java` | Lookup PESSIMISTIC_WRITE |
| `src/main/resources/templates/activate_account.html` | Template email d'activation |
| `src/main/resources/templates/reset_password.html` | Template email de reset |
| `src/main/resources/db/migration/V5__add_token_tables.sql` | Tables de tokens |
| `src/main/resources/db/migration/V6__add_refresh_tokens.sql` | Table refresh token |
