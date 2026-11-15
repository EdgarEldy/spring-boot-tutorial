# Branch: feature/user-management

This branch completes the user administration layer. It refactors `AppUser` to use the JPA
`Role` entity instead of the enum, adds three new Flyway migrations, introduces three custom
Bean Validation constraints, and exposes a full `UserController` for admin operations.

---

## What you will learn

- How to migrate a Java enum to a JPA entity with a join table managed by Flyway
- How to write a reusable field-level Bean Validation constraint (`@ValidPassword`)
- How to write a class-level cross-field constraint (`@PasswordsMatch`)
- How to write a generic database-backed uniqueness constraint (`@Unique`) that works in every test slice
- How to bootstrap an initial admin user with a `CommandLineRunner`

---

## What it delivers

### Flyway migrations

| Migration | What it does |
|---|---|
| V7 | Creates `role_user` join table, seeds `ADMIN` and `USER` roles, migrates existing users to `USER` |
| V8 | Adds `account_locked` boolean column to `app_users` |
| V9 | Adds `first_name` and `last_name` VARCHAR(100) nullable columns to `app_users` |

### AppUser refactoring

The `role` field (enum) becomes `@ManyToMany Set<Role> roles` with `@Builder.Default = new HashSet<>()`. `enabled` and `accountLocked` fields are added. `firstName` and `lastName` complete the user profile.

`UserDetailsServiceImpl` is updated to build `ROLE_*` and `PERMISSION_*` authorities from
the `Set<Role>` by iterating each role's permission set. It also checks `!enabled` and
`accountLocked` to throw the appropriate `DisabledException` or `LockedException`.

### @ValidPassword

A field-level constraint that validates:
- At least one uppercase letter
- At least one digit
- At least one special character from `@#$%^&+=!`
- Minimum 8 characters
- No whitespace

`PasswordValidator.isValid` returns `true` for `null` values, delegating the not-null check
to `@NotBlank`. This keeps responsibilities separate.

### @PasswordsMatch

A class-level constraint placed on `RegisterRequest`. `PasswordsMatchValidator` reads the
`password` and `confirmPassword` components by reflection and reports the violation on the
`confirmPassword` node via `context.buildConstraintViolationWithTemplate(...).addPropertyNode(...)`.
Reporting on the specific field (rather than the class) produces a `fieldErrors.confirmPassword`
entry in the 400 response instead of a class-level error.

### @Unique

A generic field-level constraint that queries the database via `EntityManager` to check
uniqueness. It has three attributes: `entityClass`, `fieldName`, and `caseSensitive`.

The `EntityManager` is injected with `@Autowired(required = false)` instead of
`@PersistenceContext`. `@PersistenceContext` is processed by
`PersistenceAnnotationBeanPostProcessor`, which throws `BeanCreationException` if no
`EntityManagerFactory` is present - which is the case in `@WebMvcTest` slices. `@Autowired(required = false)` silently skips the injection when the bean is absent, allowing the
validator to be constructed in any test context. A guard `if (entityManager == null) return true`
treats the constraint as passing when there is no database, leaving the database's own
unique constraint as the definitive guard.

> **TOCTOU note:** A check-then-act pattern like `@Unique` has an inherent race condition:
> two concurrent registrations with the same email can both pass the check before either
> inserts. The database unique constraint is the final safety net. `@Unique` improves the
> user experience by returning a readable 400 instead of a 500, but it does not eliminate
> the need for the database constraint.

### UserController

ADMIN-only (`@PreAuthorize("hasRole('ADMIN')")` at class level). Endpoints:

| Method | Path | Action |
|---|---|---|
| GET | `/api/v1/users` | Paginated list |
| GET | `/api/v1/users/{id}` | Single user |
| DELETE | `/api/v1/users/{id}` | Delete |
| POST | `/api/v1/users/{id}/lock` | Lock account |
| POST | `/api/v1/users/{id}/unlock` | Unlock account |
| PUT | `/api/v1/users/{id}/profile` | Update firstName/lastName |
| PATCH | `/api/v1/users/{id}/roles` | Batch add/remove roles |

### DataSeeder

`DataSeeder` is updated to seed an initial admin user when `appUserRepository.count() == 0`.
It queries `roleRepository.findByRoleName("ADMIN")` first; if the role does not exist, the
seed is skipped to avoid a foreign key error.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../validation/password/ValidPassword.java` | Annotation |
| `src/main/java/.../validation/password/PasswordValidator.java` | Implementor |
| `src/main/java/.../validation/match/PasswordsMatch.java` | Class-level annotation |
| `src/main/java/.../validation/match/PasswordsMatchValidator.java` | Cross-field check |
| `src/main/java/.../validation/unique/Unique.java` | Generic uniqueness annotation |
| `src/main/java/.../validation/unique/UniqueValidator.java` | EntityManager query |
| `src/main/java/.../service/UserService.java` | User service interface |
| `src/main/java/.../service/impl/UserServiceImpl.java` | Full user management logic |
| `src/main/java/.../controller/UserController.java` | ADMIN-only user endpoints |
| `src/main/resources/db/migration/V7__add_role_user_join_table.sql` | Join table + seed |
| `src/main/resources/db/migration/V8__add_account_locked.sql` | account_locked column |
| `src/main/resources/db/migration/V9__add_name_fields_to_app_users.sql` | Name columns |
