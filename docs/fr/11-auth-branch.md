# Branche : feature/auth

Cette branche ajoute la couche d'authentification complète : Spring Security 6, JWT sans
état, inscription des utilisateurs avec activation par email, et connexion. Elle introduit
également les deux pièges de sécurité les plus faciles à manquer dans un projet tutoriel :
les données sensibles qui fuient dans les logs, et l'impact de l'ajout d'un bean `Filter`
sur tous les tests `@WebMvcTest` existants.

---

## Ce que vous allez apprendre

- Comment configurer Spring Security 6 pour une API REST sans état (pas de sessions, pas de CSRF)
- Comment implémenter l'authentification JWT avec `jjwt` et `OncePerRequestFilter`
- Comment charger les détails utilisateur et émettre des objets `GrantedAuthority` pour les rôles et les permissions
- Comment utiliser `@AuthenticationPrincipal UserDetails` de façon sûre (et pourquoi `Authentication.getName()` n'est pas équivalent)
- Comment masquer les champs sensibles dans `toString()` pour éviter les fuites de mots de passe et de tokens dans les logs
- Comment écrire un test de sécurité de bout en bout qui utilise un vrai JWT

---

## Ce que cette branche apporte

### Entités AppUser et Role

`AppUser` stocke `username`, `email`, `password` (BCrypt), `enabled` et `accountLocked`.
Il a une relation `@ManyToMany` vers `Role`. Le `Role` initial est un enum (`USER`/`ADMIN`)
qui est remplacé plus tard par une entité JPA sur `feature/roles-permissions`.

Flyway V2 crée les tables `app_users` et `roles` et insère les rôles initiaux.

### JwtService et JwtProperties

`JwtService` construit et analyse les tokens JWT avec `jjwt` 0.12.6. Le token inclut les
claims `sub` (email), `jti` (UUID), `roles`, `permissions`, `iat` et `exp`.

`JwtProperties` est un `record` annoté `@ConfigurationProperties(prefix = "jwt")`. Il
remplace les deux champs `@Value("${jwt.secret}")` et `@Value("${jwt.expiration-ms}")` qui
disséminaient autrement la lecture de configuration dans la classe.

### UserDetailsServiceImpl

`loadUserByUsername(email)` charge l'`AppUser` avec ses rôles via `@EntityGraph`. Sans
`@EntityGraph`, accéder à `appUser.getRoles()` en dehors d'une transaction (ce qui est le
cas dans `JwtAuthFilter`) lève `LazyInitializationException`.

Authorities émises :
- `ROLE_ADMIN`, `ROLE_USER` depuis chaque nom de rôle
- `products:read`, `categories:write` etc. depuis chaque permission

### JwtAuthFilter

Étend `OncePerRequestFilter`. Extrait le token Bearer, appelle `extractEmail`, charge
`UserDetails`, valide le token et définit `UsernamePasswordAuthenticationToken` dans
`SecurityContextHolder`.

Le filtre dépend de `UserDetailsService` (l'interface Spring Security), pas de
`UserDetailsServiceImpl` (la classe concrète). C'est le seul endroit dans l'application
où injecter l'interface plutôt que l'implémentation est crucial pour la testabilité :
mocker `UserDetailsService` dans `@WebMvcTest` ne fonctionne que si la dépendance est
déclarée comme l'interface.

> **Impact sur @WebMvcTest :** Chaque classe de test `@WebMvcTest` du projet nécessite
> `@MockitoBean JwtService` et `@MockitoBean UserDetailsService` après cette branche, car
> Spring Boot inclut tous les beans `Filter` dans le contexte de test indépendamment de
> `addFilters = false`. Si vous créez un nouveau `@WebMvcTest` sans ces deux mocks, le test
> échouera au démarrage avec `NoSuchBeanDefinitionException`.

### SecurityConfig

```
Configuration sans état :
  - SessionCreationPolicy.STATELESS
  - CSRF désactivé
  - JwtAuthFilter ajouté avant UsernamePasswordAuthenticationFilter

Chemins publics :
  POST /auth/register, POST /auth/login, GET /auth/activate-account

Authentifiés :
  GET /auth/me, POST /auth/logout, /api/v1/**
```

`JwtAuthenticationEntryPoint` retourne une réponse JSON 401 (pas une redirection).
`JwtAccessDeniedHandler` retourne une réponse JSON 403.

### AuthController et masquage des DTOs

`AuthController.me()` utilise `@AuthenticationPrincipal UserDetails userDetails` et appelle
`userDetails.getUsername()`.

> **Piège @AuthenticationPrincipal :** `Authentication.getName()` est documenté comme
> retournant le nom du principal. La classe Spring Security `User` retournée par
> `UserDetailsServiceImpl` n'implémente ni `Principal` ni `AuthenticatedPrincipal` ; elle
> revient à `toString()`, qui retourne le descripteur complet `User[Username=...,
> Password=..., ...]`. Le résultat : `userRepository.findByUsername(authentication.getName())`
> ne trouve rien, car la clé de recherche est un descripteur de 200 caractères au lieu d'un
> nom d'utilisateur. Utilisez `@AuthenticationPrincipal` pour obtenir l'objet `UserDetails`
> directement.

`RegisterRequest`, `LoginRequest` et `AuthResponse` surchargent `toString()` pour masquer
les champs `password` et `token`. Sans cela, `LoggingAspect` les logue en clair dans
`logs/app.log`. `AuthDtoMaskingTest` vérifie que les surcharges sont présentes.

### SecurityAuthorizationTest

`@SpringBootTest(webEnvironment = RANDOM_PORT)` avec un vrai conteneur PostgreSQL
Testcontainers. Le test inscrit un utilisateur, se connecte, reçoit un vrai JWT et appelle
un endpoint protégé avec ce token. C'est le seul test qui exerce `JwtAuthFilter` de bout
en bout avec un token authentique.

---

## Fichiers clés

| Fichier | Rôle |
|---|---|
| `src/main/java/.../entity/user/AppUser.java` | Entité JPA utilisateur |
| `src/main/java/.../security/JwtService.java` | Génération et parsing JWT |
| `src/main/java/.../security/JwtProperties.java` | @ConfigurationProperties pour JWT |
| `src/main/java/.../security/JwtAuthFilter.java` | OncePerRequestFilter |
| `src/main/java/.../security/UserDetailsServiceImpl.java` | Chargement UserDetails |
| `src/main/java/.../security/SecurityConfig.java` | Configuration de la chaîne de filtres |
| `src/main/java/.../controller/AuthController.java` | register, login, me, logout |
| `src/main/java/.../dto/auth/RegisterRequest.java` | Avec toString() masqué |
| `src/main/java/.../dto/auth/LoginRequest.java` | Avec toString() masqué |
| `src/main/java/.../dto/auth/AuthResponse.java` | Avec toString() masqué |
| `src/test/java/.../security/AuthDtoMaskingTest.java` | Vérifie le masquage toString() |
| `src/test/java/.../security/SecurityAuthorizationTest.java` | Test JWT de bout en bout |
| `src/main/resources/db/migration/V2__init_users_and_roles.sql` | Schéma utilisateurs et rôles |
