# Authentification et autorisation

Ce document explique comment l'application authentifie les utilisateurs et applique le
contrôle d'accès avec Spring Security 6, les tokens JWT et l'autorisation basée sur les
rôles et les permissions.

---

## Vue d'ensemble

L'application utilise une architecture JWT sans état :

- Pas de stockage de session côté serveur. Chaque requête est authentifiée en validant le JWT
  dans l'en-tête `Authorization: Bearer`.
- `JwtAuthFilter` s'exécute avant chaque requête et peuple le `SecurityContext` si le token
  est valide.
- Les rôles et les permissions sont embarqués dans les claims JWT, éliminant une requête
  base de données pour la plupart des vérifications d'autorisation.

---

## Flux d'authentification

```
1. POST /auth/register
   - Enregistre AppUser avec enabled=false
   - Crée un ActivationToken, envoie l'email d'activation via EmailService (@Async)

2. GET /auth/activate-account?token=<token>
   - Valide l'ActivationToken, passe enabled à true

3. POST /auth/login
   - Vérifie le mot de passe (BCrypt), contrôle enabled et accountLocked
   - Retourne access_token (JWT), refresh_token, expires_in

4. Requêtes suivantes
   - JwtAuthFilter extrait le jti du token
   - Vérifie la table BlacklistedToken (PESSIMISTIC_WRITE)
   - Charge UserDetails, peuple le SecurityContext

5. POST /auth/logout
   - Blackliste le jti du token dans la table BlacklistedToken
```

---

## Structure du JWT

`JwtService.generateToken(AppUser)` construit un token avec ces claims :

| Claim | Valeur |
|---|---|
| `sub` | Email de l'utilisateur |
| `jti` | UUID (identifiant unique du token, utilisé pour le blacklistage) |
| `roles` | Liste des noms de rôles (ex. `["ADMIN", "USER"]`) |
| `permissions` | Liste des permissions (ex. `["products:read"]`) |
| `iat` | Horodatage d'émission |
| `exp` | Horodatage d'expiration |

`JwtProperties` (`@ConfigurationProperties(prefix = "jwt")`) lit `jwt.secret` et
`jwt.expiration-ms` depuis `application.yml`, remplaçant les annotations `@Value` et
permettant une validation par construction.

---

## JwtAuthFilter

`JwtAuthFilter` étend `OncePerRequestFilter`. Pour chaque requête :

1. Lit l'en-tête `Authorization` ; ignore si absent ou sans `Bearer`.
2. Appelle `jwtService.extractEmail(token)` pour obtenir le nom d'utilisateur.
3. Appelle `blacklistedTokenService.isBlacklisted(jti)` - enveloppé dans un `try/catch
   DataAccessException` pour qu'une panne de base de données ne casse pas l'authentification.
4. Charge `UserDetails` via `UserDetailsService`.
5. Appelle `jwtService.isTokenValid(token, userDetails)`.
6. Définit `UsernamePasswordAuthenticationToken` dans `SecurityContextHolder`.

> **Impact sur @WebMvcTest :** `JwtAuthFilter` est un `Filter`. Le slice `@WebMvcTest` de
> Spring Boot inclut tous les beans `Filter` du contexte indépendamment de `addFilters = false`
> (ce flag contrôle l'exécution, pas la construction). Chaque classe de test `@WebMvcTest`
> doit ajouter `@MockitoBean JwtService` et `@MockitoBean UserDetailsService` pour permettre
> la construction de `JwtAuthFilter`, même si le test n'utilise pas l'authentification.

---

## SecurityConfig

```
Endpoints publics (aucun token requis) :
  POST /auth/register
  POST /auth/login
  GET  /auth/activate-account
  POST /auth/forgot-password
  POST /auth/reset-password

Endpoints authentifiés :
  GET /auth/me
  POST /auth/logout
  /api/v1/** (toutes les ressources)

Contrôleurs réservés ADMIN (via @PreAuthorize au niveau classe) :
  /api/v1/users/**
  /api/v1/roles/**
  /api/v1/permissions/**
```

CSRF est désactivé pour l'API REST (sans état, pas de session navigateur). La création de
session est configurée en `STATELESS`.

---

## Contrôle d'accès basé sur les rôles

`UserDetailsServiceImpl.loadUserByUsername()` charge l'`AppUser` avec son `Set<Role>` et
les `Set<Permission>` de chaque rôle, puis émet deux types de `GrantedAuthority` :

- `ROLE_ADMIN`, `ROLE_USER` - pour les vérifications `hasRole()` dans `SecurityConfig` et `@PreAuthorize`
- `products:read`, `categories:write` - pour les vérifications `hasPermission()` via `CustomPermissionEvaluator`

`@EnableMethodSecurity` est activé dans `MethodSecurityConfig`, qui enregistre
`CustomPermissionEvaluator`. Cela permet des annotations précises comme :

```java
@PreAuthorize("hasPermission(null, 'products', 'write')")
public ResponseEntity<...> create(...) { ... }
```

---

## Cycle de vie des tokens

| Token | Stockage | Rôle |
|---|---|---|
| Access token | Client uniquement (mémoire ou localStorage) | De courte durée, authentifie les appels API |
| Refresh token | PostgreSQL (table `refresh_tokens`) | De longue durée, permet d'obtenir un nouveau access token |
| Blacklisted token | PostgreSQL (table `blacklisted_tokens`) | Access tokens révoqués (logout) |
| Activation token | PostgreSQL (table `activation_tokens`) | Vérification d'email après inscription |
| Password reset token | PostgreSQL (table `password_reset_tokens`) | Flux de réinitialisation du mot de passe |
