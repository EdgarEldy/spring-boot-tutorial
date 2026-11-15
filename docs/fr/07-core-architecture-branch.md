# Branche : feature/core-architecture

C'est la première branche. Elle construit le squelette technique dont toutes les branches
suivantes dépendent. Aucune ressource métier n'est implémentée ici ; l'objectif est une
infrastructure opérationnelle : enveloppes de réponse, gestion des erreurs, aspects AOP,
configuration du cache, structure de sécurité, et setup Docker + CI.

---

## Ce que vous allez apprendre

- Comment construire une enveloppe de réponse cohérente avec `ApiResponse<T>` et `PageResponse<T>`
- Comment centraliser la gestion des erreurs avec `@RestControllerAdvice` et `ProblemDetail` (RFC 7807)
- Comment utiliser les quatre types d'advice AOP : `@Before`, `@After`, `@AfterReturning`, `@AfterThrowing` et `@Around`
- Comment Spring AOP matche les méthodes proxy sur toute la hiérarchie d'interfaces (pas seulement la classe déclarante)
- Comment configurer Caffeine comme cache nommé avec TTL et limite de taille via `CacheConfig`
- Comment lier des propriétés de configuration à un record type-safe avec `@ConfigurationProperties`
- Comment exposer un endpoint Actuator personnalisé avec `@Endpoint`
- Comment configurer CORS, la documentation OpenAPI et l'audit JPA comme infrastructure partagée
- Comment conteneuriser l'application avec Docker Compose et automatiser le CI avec GitHub Actions

---

## Ce que cette branche apporte

### ApiResponse et PageResponse

`ApiResponse<T>` est l'enveloppe de toutes les réponses de l'application - succès et erreurs.
Elle contient `success`, `message`, `data` et `timestamp`. Les méthodes de fabrique
`ApiResponse.success(message, data)` et `ApiResponse.error(message, data)` assurent une
construction cohérente dans tous les contrôleurs et gestionnaires d'exceptions.

`PageResponse<T>` est utilisé comme champ `data` pour les listes paginées. Il encapsule le
`Page<T>` de Spring Data et expose `content`, `page`, `size`, `totalElements`, `totalPages`
et `last`.

### GlobalExceptionHandler

`@RestControllerAdvice` avec une méthode `@ExceptionHandler` par type d'exception. Chaque
gestionnaire retourne `ResponseEntity<ApiResponse<ProblemDetail>>`, utilisant la classe
`ProblemDetail` native de Spring 6 plutôt qu'un DTO personnalisé. Les erreurs de validation
(400) ajoutent une map d'extension `fieldErrors` à l'objet `ProblemDetail`.

### LoggingAspect

Pointcut `@Around` sur `execution(* com.edgareldy.springboottutorial.service..*(..))`.
Logue le nom de la méthode, les arguments, la valeur de retour et le temps d'exécution au
niveau INFO avant et après chaque appel de service. Les records qui contiennent des mots de
passe ou des tokens doivent surcharger `toString()` pour masquer les champs sensibles - voir
[06-conventions.md](06-conventions.md).

### ExecutionTimeAspect

Pointcut `@Around` sur toutes les méthodes de contrôleurs. Logue l'endpoint HTTP, le nom
de la méthode et le temps d'exécution réel. Utile pour identifier les endpoints lents.

### RepositoryAuditAspect

Utilise les quatre types d'advice sur `execution(* com.edgareldy.springboottutorial.repository..*(..))` :

- `@Before` - logue la méthode appelée avec ses arguments
- `@AfterReturning` - logue la valeur de retour
- `@After` - logue la complétion (s'exécute toujours, même en cas d'exception)
- `@AfterThrowing` - logue le message de l'exception

> **Spring AOP et les hiérarchies d'interfaces :** Un proxy créé pour `CategoryRepository`
> (une interface vide étendant `JpaRepository`) délègue à l'implémentation Spring Data. Lors
> du matching, Spring AOP considère la hiérarchie complète d'interfaces du proxy, pas
> seulement la classe déclarante de la méthode. Cela signifie que `@Before` sur `repository`
> s'active pour `save()`, `findById()` et les autres méthodes déclarées sur `CrudRepository`,
> même si ces méthodes ne sont pas déclarées dans le package `repository`. Cela a été vérifié
> empiriquement - une analyse théorique basée sur `Method.getDeclaringClass()` prédisait que
> cela ne fonctionnerait pas, mais c'est le cas.

### CacheConfig

Configure Caffeine comme implémentation de support pour le `CacheManager` de Spring. Deux
caches nommés sont définis : `categories` et `products`. Chaque cache a une taille maximale
de 500 entrées et une durée de vie de 10 minutes. Ces caches sont consommés par
`CategoryServiceImpl` et `ProductServiceImpl` sur `feature/products`.

### CorsConfig

Autorise les requêtes cross-origin depuis `http://localhost:4200` (port par défaut du serveur
de dev Angular). Actif uniquement quand le profil `dev` est chargé.

### AppInfoEndpoint

Un endpoint Actuator personnalisé annoté `@Endpoint(id = "appinfo")`. Exposé uniquement dans
le profil `dev` via `management.endpoints.web.exposure.include`. Montre comment ajouter des
endpoints Actuator personnalisés au-delà des endpoints intégrés.

### OpenApiConfig

Configure springdoc-openapi 2.8.16 avec un schéma de sécurité JWT `BearerAuth`. Cela prépare
Swagger UI à afficher un bouton Authorize qui injecte l'en-tête `Authorization: Bearer` sur
les requêtes authentifiées, utilisé à partir de `feature/auth`.

### JpaAuditingConfig

Active `@EnableJpaAuditing`, qui active les annotations `@CreatedDate` et `@LastModifiedDate`
de Spring Data sur les champs d'entités. Configuré ici pour être disponible dans toutes les
branches suivantes sans dupliquer l'annotation.

### Docker et CI

`docker-compose.yml` démarre PostgreSQL 16 et MailHog avec l'application. Toutes les valeurs
sensibles (`POSTGRES_*`, `DB_*`, `JWT_SECRET`) sont lues depuis des variables d'environnement
avec des valeurs par défaut sécurisées via `${VAR:-default}`. `.env.example` documente les
variables requises sans committer de vrais secrets.

`.github/workflows/ci.yml` s'exécute à chaque push sur `develop` et `master`, et à chaque
pull request ciblant ces branches : `mvn clean test` avec Testcontainers.

---

## Fichiers clés

| Fichier | Rôle |
|---|---|
| `src/main/java/.../dto/common/ApiResponse.java` | Record enveloppe de réponse |
| `src/main/java/.../dto/common/PageResponse.java` | Record wrapper liste paginée |
| `src/main/java/.../exception/GlobalExceptionHandler.java` | Gestion centralisée des erreurs |
| `src/main/java/.../exception/ResourceNotFoundException.java` | Exception 404 |
| `src/main/java/.../exception/BusinessRuleException.java` | Exception 422 |
| `src/main/java/.../aspect/LoggingAspect.java` | Logging au niveau service |
| `src/main/java/.../aspect/ExecutionTimeAspect.java` | Temps d'exécution contrôleur |
| `src/main/java/.../aspect/RepositoryAuditAspect.java` | Audit au niveau repository |
| `src/main/java/.../config/CacheConfig.java` | Configuration du cache Caffeine |
| `src/main/java/.../config/CorsConfig.java` | CORS pour le profil dev |
| `src/main/java/.../config/OpenApiConfig.java` | Configuration Swagger UI |
| `src/main/java/.../config/JpaAuditingConfig.java` | Activation de l'audit JPA |
| `src/main/java/.../config/AppInfoEndpoint.java` | Endpoint Actuator personnalisé |
| `docker-compose.yml` | Stack de développement local |
| `.env.example` | Modèle de variables d'environnement |
| `.github/workflows/ci.yml` | Pipeline GitHub Actions |
