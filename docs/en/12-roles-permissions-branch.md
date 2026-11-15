# Branch: feature/roles-permissions

This branch replaces the `Role` enum from `feature/auth` with two full JPA entities: `Role`
and `Permission`. It also adds method-level security (`@PreAuthorize`) and a reusable batch
add/remove pattern for many-to-many relationships.

---

## What you will learn

- How to replace a Java enum with a JPA entity when roles need to be managed at runtime
- How to enable method-level security with `@EnableMethodSecurity` and `@PreAuthorize`
- How to implement a custom `PermissionEvaluator` for `hasPermission()` expressions
- How to avoid N+1 on a paginated `findAll` that includes a `@ManyToMany` collection
- How to design a PATCH endpoint for batch add/remove on a many-to-many relationship

---

## What it delivers

### Permission and Role entities

`Permission` has `resource` and `action` fields with a unique constraint on the combination.
`Role` has a `roleName` field and a `@ManyToMany Set<Permission>`.

Flyway V5 creates the `permissions`, `roles`, and `role_permissions` tables.

### CRUD controllers (ADMIN-only)

`RoleController` and `PermissionController` are annotated
`@PreAuthorize("hasRole('ADMIN')")` at the class level. This applies the check to every
method in the class without repeating the annotation on each one.

### MethodSecurityConfig and CustomPermissionEvaluator

`MethodSecurityConfig` is a `@Configuration` class that also carries `@EnableMethodSecurity`.
It registers `CustomPermissionEvaluator` as the bean that handles `hasPermission()`
expressions.

`CustomPermissionEvaluator.hasPermission(Authentication, Object, Object)` loads the
authenticated user's permissions and checks whether any permission matches
`resource:action`. This enables fine-grained endpoint protection:

```java
@PreAuthorize("hasPermission(null, 'products', 'write')")
```

### PATCH batch add/remove

`PATCH /api/v1/roles/{roleId}/permissions` accepts an `UpdateRolePermissionsRequest`:

```json
{ "add": [2, 3], "remove": [1] }
```

Both fields are optional, but at least one must be non-empty. An id present in both `add`
and `remove` is rejected with a 400. `@AssertTrue` on private accessor methods of the record
implements these cross-field constraints without a custom validator class.

The service loads all permissions for the union of ids in one `findAllById` call. If any id
is not found, a 404 listing the unknown ids is returned before any modification is made.
Adding a permission the role already has, or removing one it does not have, is a no-op
(idempotent semantics).

> **Why PATCH and not PUT?** A `PUT` replaces the entire permissions list. If two admins
> manage the same role concurrently, one admin's changes overwrite the other's. A `PATCH`
> with explicit `add`/`remove` sets is order-independent and safe to call concurrently.

### @EntityGraph on paginated findAll

`RoleRepository.findAll(Pageable)` is overridden with `@EntityGraph(attributePaths =
"permissions")`. Without it, Hibernate issues one select for each role to load its
permissions - a classic N+1 on a paginated list.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../entity/Role.java` | Role JPA entity with @ManyToMany permissions |
| `src/main/java/.../entity/Permission.java` | Permission JPA entity |
| `src/main/java/.../security/MethodSecurityConfig.java` | @EnableMethodSecurity |
| `src/main/java/.../security/CustomPermissionEvaluator.java` | hasPermission() logic |
| `src/main/java/.../service/RoleService.java` | Role service interface |
| `src/main/java/.../service/impl/RoleServiceImpl.java` | Batch add/remove logic |
| `src/main/java/.../controller/RoleController.java` | ADMIN-only CRUD + PATCH |
| `src/main/java/.../controller/PermissionController.java` | ADMIN-only CRUD |
| `src/main/java/.../dto/role/UpdateRolePermissionsRequest.java` | @AssertTrue validation |
| `src/main/resources/db/migration/V5__add_roles_and_permissions.sql` | Schema |
