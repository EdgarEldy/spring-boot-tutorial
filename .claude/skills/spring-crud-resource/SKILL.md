---
name: spring-crud-resource
description: Implémente un CRUD complet (entité, repository, DTOs, mapper, service contrat/impl, controller, tests) pour une ressource du modèle de données (Category, Product, Customer, Order ou toute nouvelle ressource), en respectant strictement les conventions du projet spring-boot-tutorial. Déclenche cette skill dès qu'on demande d'ajouter/générer/implémenter une ressource, une entité, un endpoint CRUD, ou une branche feature/products, feature/customers, feature/orders.
---

# CRUD Spring Boot — conventions du projet

Cette skill décrit l'ordre et les règles strictes à suivre pour implémenter le CRUD complet
d'une ressource du modèle de données (`categories`, `products`, `customers`, `orders`, ou une
future ressource), conformément au `README.md` (§ Modèle de données, § Structure du projet,
§ Format de réponse standard, § Conventions de code) et au `.claude/CLAUDE.md` du projet.

Package racine : `edgareldy.springboottutorial`.

**Règle absolue : ne jamais exposer une entité JPA (`@Entity`) directement dans une réponse
HTTP.** Les controllers ne manipulent que des DTOs (`record`), jamais les classes de `entity/`.

## Étapes

### 1. Entité JPA — `entity/<Resource>.java`

- Classe annotée `@Entity`, avec Lombok (`@Getter`/`@Setter` ou `@Data` selon le reste du
  codebase — vérifier les entités existantes pour rester cohérent).
- Colonnes et contraintes reprises exactement du README (§ Modèle de données) :
  types, `NOT NULL`, `UNIQUE`, valeurs par défaut, format.
- Relations (`@ManyToOne`, `@OneToMany`) en chargement **`LAZY`**, jamais `EAGER`.
- Les contraintes de validation métier (ex : `unit_price > 0`, `quantity > 0`, format email)
  sont posées au niveau du schéma (Flyway) **et** au niveau du DTO Request (Bean Validation),
  pas uniquement sur l'entité.
- Migration Flyway correspondante dans `src/main/resources/db/migration/` si la table
  n'existe pas encore (`V1__init_schema.sql` ou nouveau script versionné).

### 2. Repository — `repository/<Resource>Repository.java`

- Interface Spring Data JPA (`extends JpaRepository<Entity, Long>`).
- Méthodes dérivées nécessaires (ex : `findByCategoryId`, `findByEmail`).
- Au moins une requête `@Query` avec `JOIN FETCH` ou `JOIN` explicite si la ressource a des
  relations à charger pour les listes (éviter le N+1).

### 3. DTOs — `dto/<resource>/<Resource>Request.java` et `<Resource>Response.java`

- **Toujours en `record` Java**, jamais en classe.
- `XxxRequest` : uniquement les champs modifiables par le client, annotés Bean Validation
  (`@NotNull`, `@NotBlank`, `@Positive`, `@Email`, etc.) selon les contraintes du README.
- `XxxResponse` : représentation exposée à l'API — inclut les sous-objets résumés si la
  ressource a des relations (ex : `OrderResponse` contient un résumé de `customer`/`product`,
  pas l'entité complète).
- Un dossier par ressource sous `dto/`, au singulier minuscule (ex : `dto/product/`).

### 4. Mapper — `mapper/<Resource>Mapper.java`

- Interface MapStruct (`@Mapper(componentModel = "spring")`).
- Méthodes de conversion entité ↔ DTO dans les deux sens nécessaires au service
  (`toEntity(Request)`, `toResponse(Entity)`, éventuellement `updateEntityFromRequest`).
- Vérifier l'ordre des annotation processors dans `pom.xml` (Lombok avant MapStruct) — ne pas
  le modifier sauf si la compilation échoue à cause de ça.

### 5. Service — contrat + implémentation

- Interface `XxxService` à la **racine** de `service/` (`service/XxxService.java`) : définit le
  contrat métier (méthodes CRUD + règles métier spécifiques à la ressource).
- Implémentation `XxxServiceImpl` dans `service/impl/` (`service/impl/XxxServiceImpl.java`) :
  - annotée `@Service`
  - injecte `XxxRepository` et `XxxMapper`
  - **`@Transactional` sur chaque méthode d'écriture** (create/update/delete), pas sur les
    lectures simples
  - lève `ResourceNotFoundException` si la ressource n'existe pas (404)
  - lève `BusinessRuleException` pour toute règle métier violée (422) — ex : suppression d'une
    catégorie non vide, email client déjà utilisé, total de commande incohérent
- **Les controllers et les tests injectent toujours l'interface `XxxService`, jamais
  `XxxServiceImpl` directement.**

### 6. Controller — `controller/<Resource>Controller.java`

- Aucune logique métier : uniquement validation des entrées (`@Valid`) et délégation au
  service via l'interface.
- **Toutes les réponses sont enveloppées dans `ApiResponse<T>`** (`dto/common/ApiResponse.java`) :
  - `GET /{id}`, `POST`, `PUT` → `ApiResponse<XxxResponse>`
  - `GET` (liste) → `ApiResponse<PageResponse<XxxResponse>>`, jamais une `List<XxxResponse>` nue
  - `DELETE` → `ApiResponse<Void>`
- Endpoints REST au pluriel, `kebab-case` si composés (ex : `/api/categories`,
  `/api/customers/{id}/orders`).
- Pagination/tri via `Pageable` pour les listes, filtres en `@RequestParam` optionnels
  (ex : `?categoryId=`, `?search=`).
- Chaque endpoint documenté avec `@Operation(summary = "...")` (springdoc-openapi).

### 7. Tests

- **Tests unitaires du service** (`src/test/.../service/XxxServiceImplTest.java`) : Mockito,
  mock du repository et du mapper, un test par méthode publique du service et par règle
  métier (cas nominal + cas d'erreur : ressource introuvable, règle métier violée).
- **Tests d'intégration du controller** (`src/test/.../controller/XxxControllerTest.java`) :
  MockMvc (`@WebMvcTest` ou `@SpringBootTest` selon le reste du projet), un test par endpoint
  couvrant le cas nominal, la validation (400), le 404, et les règles métier (422) pertinentes.
- Vérifier que le JSON de réponse dans les tests d'intégration correspond bien à l'enveloppe
  `ApiResponse<T>` (`success`, `message`, `data`, `timestamp`).

## Ordre de vérification avant de considérer la ressource terminée

1. L'entité ne fuit jamais dans une réponse HTTP (grep rapide : le controller et le mapper ne
   retournent que des DTOs).
2. Le controller ne dépend que de l'interface `XxxService`.
3. `@Transactional` présent sur toutes les méthodes d'écriture de `XxxServiceImpl`.
4. Toutes les réponses du controller passent par `ApiResponse<T>` (ou `ApiResponse<PageResponse<T>>`).
5. Tests unitaires + tests d'intégration présents et verts (`./mvnw test`).
6. Mettre à jour `.claude/CLAUDE.md` (état d'avancement de la branche, écarts éventuels) une
   fois la ressource terminée.
