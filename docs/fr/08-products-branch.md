# Branche : feature/products

Cette branche ajoute les deux premières ressources métier : Category et Product. Elle
établit le pattern CRUD que toutes les branches fonctionnelles suivantes répètent. Si vous
comprenez comment fonctionnent les catégories et les produits, vous comprenez comment
fonctionne chaque autre ressource de liste.

---

## Ce que vous allez apprendre

- Comment mettre en cache le résultat de lookups individuels avec `@Cacheable` et invalider le cache lors des mutations avec `@CacheEvict`
- Comment pré-peupler des données de développement avec `CommandLineRunner` et une garde d'idempotence
- Comment détecter et rejeter les mises à jour concurrentes avec le verrouillage optimiste (`@Version`)
- Comment construire un filtre de requête dynamique avec `JpaSpecificationExecutor` et `Specification`
- Comment enregistrer les horodatages `created_at` / `updated_at` automatiquement avec les annotations d'audit JPA

---

## Ce que cette branche apporte

### CRUD Category

`CategoryService` / `CategoryServiceImpl` gère le cycle de vie CRUD complet. `findById` est
annoté `@Cacheable(value = "categories", key = "#id")` pour que les lookups répétés sur le
même id soient servis depuis Caffeine. `update` et `delete` sont annotés `@CacheEvict` pour
supprimer l'entrée obsolète quand la catégorie change.

`CategoryResponse` gagne les champs `createdAt` et `updatedAt` (Flyway V4 ajoute les
colonnes, `@CreatedDate` / `@LastModifiedDate` sur le champ d'entité les peuple via
`JpaAuditingConfig`).

### CRUD Product

`ProductService` / `ProductServiceImpl` suit le même pattern. `findByIdWithCategory` utilise
`@EntityGraph(attributePaths = "category")` pour charger la catégorie en avance dans une
seule requête, évitant le problème N+1 sur l'endpoint de détail.

`@Cacheable` et `@CacheEvict` sont appliqués à `findById`, `update` et `delete`. L'endpoint
de liste paginée `findAll` n'est pas mis en cache, car le jeu de résultats change avec chaque
combinaison de paramètres de tri/filtre et le taux de succès du cache serait proche de zéro.

### Verrouillage optimiste avec @Version

`Product` possède un champ `version` annoté `@Version`. Flyway V3 ajoute la colonne. Lorsque
deux requêtes concurrentes mettent à jour le même produit, Hibernate détecte la divergence de
version et lève `OptimisticLockingFailureException`. `GlobalExceptionHandler` le traduit en
409 Conflict.

> **Pourquoi optimiste et pas pessimiste ?** Le verrouillage pessimiste maintient un verrou
> sur la ligne de base de données pendant toute la durée de la requête. Pour une API REST où
> les requêtes individuelles sont courtes et les modifications concurrentes rares, le
> verrouillage optimiste est moins coûteux : aucun verrou n'est maintenu, et les conflits ne
> sont détectés que lorsqu'ils se produisent réellement.

### ProductSpecifications

`ProductSpecifications` fournit des méthodes de fabrique statiques qui retournent des
prédicats `Specification<Product>` : `hasNameLike(String)`,
`hasPriceGreaterThanOrEqual(BigDecimal)`, `hasPriceLessThanOrEqual(BigDecimal)`.

`ProductRepository` étend `JpaSpecificationExecutor<Product>`. `ProductServiceImpl.findAll`
combine les prédicats avec `Specification.where(...).and(...)` lorsqu'un paramètre de filtre
est présent, en revenant à la requête paginée standard si aucun n'est fourni.

> **Problème N+1 sur le chemin Specification :** La requête `Specification` n'utilise pas
> `@EntityGraph`, donc `Product.category` est chargé de façon paresseuse. Si la réponse
> inclut `categoryName`, chaque produit de la page déclenche un select supplémentaire.
> Ceci est documenté et accepté pour le chemin de filtre, utilisé pour la recherche ciblée,
> pas la navigation de pages complètes.

### DataSeeder

`DataSeeder` implémente `CommandLineRunner` et est annoté `@Profile("dev")`. Il s'exécute
une fois au démarrage. Une garde d'idempotence (`categoryRepository.count() == 0`) garantit
qu'il n'insère des données que lorsque la base est vide, ce qui permet de redémarrer
l'application sans dupliquer les données.

Il insère deux catégories (Electronics, Books) et trois produits.

### Isolation des tests de cache

`CategoryServiceCacheTest` et `ProductServiceCacheTest` utilisent `@SpringBootTest` avec
Testcontainers. Spring réutilise le même contexte d'application entre les méthodes de test
dans la même classe, ce qui signifie que la même instance de `CacheManager` est partagée.
Sans nettoyage, une entrée écrite par un test persiste et affecte les assertions du suivant.
Les deux classes de test nettoient leurs caches respectifs dans une méthode `@BeforeEach`.

---

## Fichiers clés

| Fichier | Rôle |
|---|---|
| `src/main/java/.../entity/Category.java` | Entité JPA Category avec audit JPA |
| `src/main/java/.../entity/Product.java` | Entité JPA Product avec @Version |
| `src/main/java/.../service/CategoryService.java` | Interface de service Category |
| `src/main/java/.../service/impl/CategoryServiceImpl.java` | Implémentation avec @Cacheable |
| `src/main/java/.../service/ProductService.java` | Interface de service Product |
| `src/main/java/.../service/impl/ProductServiceImpl.java` | Implémentation avec @Cacheable |
| `src/main/java/.../repository/ProductSpecifications.java` | Prédicats de filtre dynamique |
| `src/main/java/.../config/DataSeeder.java` | Données de démarrage (profil dev) |
| `src/main/resources/db/migration/V3__add_version_to_products.sql` | Colonne @Version |
| `src/main/resources/db/migration/V4__add_audit_columns_to_categories.sql` | Horodatages d'audit |
| `src/test/java/.../service/CategoryServiceCacheTest.java` | Test d'intégration du cache |
| `src/test/java/.../service/ProductServiceCacheTest.java` | Test d'intégration du cache |
