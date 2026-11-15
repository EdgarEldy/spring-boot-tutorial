# Branche : feature/roles-permissions

Cette branche remplace l'enum `Role` de `feature/auth` par deux entités JPA complètes :
`Role` et `Permission`. Elle ajoute également la sécurité au niveau des méthodes
(`@PreAuthorize`) et un pattern réutilisable d'ajout/suppression en lot pour les relations
many-to-many.

---

## Ce que vous allez apprendre

- Comment remplacer un enum Java par une entité JPA lorsque les rôles doivent être gérés à l'exécution
- Comment activer la sécurité au niveau des méthodes avec `@EnableMethodSecurity` et `@PreAuthorize`
- Comment implémenter un `PermissionEvaluator` personnalisé pour les expressions `hasPermission()`
- Comment éviter le N+1 sur un `findAll` paginé qui inclut une collection `@ManyToMany`
- Comment concevoir un endpoint PATCH pour l'ajout/suppression en lot sur une relation many-to-many

---

## Ce que cette branche apporte

### Entités Permission et Role

`Permission` a les champs `resource` et `action` avec une contrainte d'unicité sur la
combinaison. `Role` a un champ `roleName` et un `@ManyToMany Set<Permission>`.

Flyway V5 crée les tables `permissions`, `roles` et `role_permissions`.

### Contrôleurs CRUD (réservés ADMIN)

`RoleController` et `PermissionController` sont annotés
`@PreAuthorize("hasRole('ADMIN')")` au niveau de la classe. Cela applique la vérification à
chaque méthode de la classe sans répéter l'annotation sur chacune.

### MethodSecurityConfig et CustomPermissionEvaluator

`MethodSecurityConfig` est une classe `@Configuration` qui porte également
`@EnableMethodSecurity`. Elle enregistre `CustomPermissionEvaluator` comme bean gérant les
expressions `hasPermission()`.

`CustomPermissionEvaluator.hasPermission(Authentication, Object, Object)` charge les
permissions de l'utilisateur authentifié et vérifie si une permission correspond à
`resource:action`. Cela permet une protection précise des endpoints :

```java
@PreAuthorize("hasPermission(null, 'products', 'write')")
```

### PATCH ajout/suppression en lot

`PATCH /api/v1/roles/{roleId}/permissions` accepte un `UpdateRolePermissionsRequest` :

```json
{ "add": [2, 3], "remove": [1] }
```

Les deux champs sont optionnels, mais au moins l'un doit être non vide. Un id présent dans
`add` et `remove` à la fois est rejeté avec un 400. `@AssertTrue` sur des méthodes
accesseurs privées du record implémente ces contraintes multi-champs sans classe de
validateur personnalisée.

Le service charge toutes les permissions pour l'union des ids en un seul appel
`findAllById`. Si un id est inconnu, un 404 listant les ids inconnus est retourné avant toute
modification. Ajouter une permission que le rôle a déjà, ou retirer une permission qu'il
n'a pas, est un no-op (sémantique idempotente).

> **Pourquoi PATCH et pas PUT ?** Un `PUT` remplace la liste complète des permissions. Si
> deux admins gèrent le même rôle simultanément, les modifications d'un admin écrasent celles
> de l'autre. Un `PATCH` avec des ensembles explicites `add`/`remove` est indépendant de
> l'ordre et sûr à appeler simultanément.

### @EntityGraph sur findAll paginé

`RoleRepository.findAll(Pageable)` est surchargé avec `@EntityGraph(attributePaths =
"permissions")`. Sans cela, Hibernate émet un select pour chaque rôle pour charger ses
permissions - un N+1 classique sur une liste paginée.

---

## Fichiers clés

| Fichier | Rôle |
|---|---|
| `src/main/java/.../entity/Role.java` | Entité JPA Role avec @ManyToMany permissions |
| `src/main/java/.../entity/Permission.java` | Entité JPA Permission |
| `src/main/java/.../security/MethodSecurityConfig.java` | @EnableMethodSecurity |
| `src/main/java/.../security/CustomPermissionEvaluator.java` | Logique hasPermission() |
| `src/main/java/.../service/RoleService.java` | Interface de service Role |
| `src/main/java/.../service/impl/RoleServiceImpl.java` | Logique ajout/suppression en lot |
| `src/main/java/.../controller/RoleController.java` | CRUD + PATCH réservés ADMIN |
| `src/main/java/.../controller/PermissionController.java` | CRUD réservé ADMIN |
| `src/main/java/.../dto/role/UpdateRolePermissionsRequest.java` | Validation @AssertTrue |
| `src/main/resources/db/migration/V5__add_roles_and_permissions.sql` | Schéma |
