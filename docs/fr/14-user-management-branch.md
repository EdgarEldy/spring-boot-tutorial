# Branche : feature/user-management

Cette branche complète la couche d'administration des utilisateurs. Elle refactorise
`AppUser` pour utiliser l'entité JPA `Role` au lieu de l'enum, ajoute trois nouvelles
migrations Flyway, introduit trois contraintes Bean Validation personnalisées et expose un
`UserController` complet pour les opérations d'administration.

---

## Ce que vous allez apprendre

- Comment migrer un enum Java vers une entité JPA avec une table de jointure gérée par Flyway
- Comment écrire une contrainte Bean Validation réutilisable au niveau d'un champ (`@ValidPassword`)
- Comment écrire une contrainte multi-champs au niveau de la classe (`@PasswordsMatch`)
- Comment écrire une contrainte d'unicité générique s'appuyant sur la base de données (`@Unique`) qui fonctionne dans tous les slices de test
- Comment démarrer l'application avec un utilisateur admin initial via `CommandLineRunner`

---

## Ce que cette branche apporte

### Migrations Flyway

| Migration | Ce qu'elle fait |
|---|---|
| V7 | Crée la table de jointure `role_user`, insère les rôles `ADMIN` et `USER`, migre les utilisateurs existants vers `USER` |
| V8 | Ajoute la colonne booléenne `account_locked` à `app_users` |
| V9 | Ajoute les colonnes `first_name` et `last_name` VARCHAR(100) nullable à `app_users` |

### Refactoring d'AppUser

Le champ `role` (enum) devient `@ManyToMany Set<Role> roles` avec
`@Builder.Default = new HashSet<>()`. Les champs `enabled` et `accountLocked` sont ajoutés.
`firstName` et `lastName` complètent le profil utilisateur.

`UserDetailsServiceImpl` est mis à jour pour construire les authorities `ROLE_*` et
`PERMISSION_*` depuis le `Set<Role>` en itérant les permissions de chaque rôle. Il vérifie
également `!enabled` et `accountLocked` pour lever `DisabledException` ou `LockedException`.

### @ValidPassword

Une contrainte de niveau champ qui valide :
- Au moins une lettre majuscule
- Au moins un chiffre
- Au moins un caractère spécial parmi `@#$%^&+=!`
- Minimum 8 caractères
- Pas d'espace

`PasswordValidator.isValid` retourne `true` pour les valeurs `null`, déléguant la vérification
"non nul" à `@NotBlank`. Cela sépare clairement les responsabilités.

### @PasswordsMatch

Une contrainte de niveau classe placée sur `RegisterRequest`. `PasswordsMatchValidator` lit
les composants `password` et `confirmPassword` par réflexion et rapporte la violation sur le
noeud `confirmPassword` via
`context.buildConstraintViolationWithTemplate(...).addPropertyNode(...)`. Rapporter sur le
champ spécifique (plutôt que sur la classe) produit une entrée
`fieldErrors.confirmPassword` dans la réponse 400 au lieu d'une erreur de niveau classe.

### @Unique

Une contrainte générique de niveau champ qui interroge la base de données via `EntityManager`
pour vérifier l'unicité. Elle a trois attributs : `entityClass`, `fieldName` et
`caseSensitive`.

L'`EntityManager` est injecté avec `@Autowired(required = false)` plutôt que
`@PersistenceContext`. `@PersistenceContext` est traité par
`PersistenceAnnotationBeanPostProcessor`, qui lève `BeanCreationException` si aucune
`EntityManagerFactory` n'est disponible - ce qui est le cas dans les slices `@WebMvcTest`.
`@Autowired(required = false)` saute silencieusement l'injection lorsque le bean est absent,
permettant au validateur d'être construit dans n'importe quel contexte de test. Une garde
`if (entityManager == null) return true` traite la contrainte comme valide en l'absence de
base de données, laissant la contrainte unique de la base de données comme garde-fou définitif.

> **Note TOCTOU :** Un pattern vérification-puis-action comme `@Unique` a une condition de
> course inhérente : deux inscriptions concurrentes avec le même email peuvent toutes deux
> passer la vérification avant que l'une n'insère. La contrainte unique de la base de données
> est le filet de sécurité final. `@Unique` améliore l'expérience utilisateur en retournant
> un 400 lisible plutôt qu'un 500, mais n'élimine pas le besoin de la contrainte de base de
> données.

### UserController

Réservé ADMIN (`@PreAuthorize("hasRole('ADMIN')")` au niveau classe). Endpoints :

| Méthode | Chemin | Action |
|---|---|---|
| GET | `/api/v1/users` | Liste paginée |
| GET | `/api/v1/users/{id}` | Utilisateur unique |
| DELETE | `/api/v1/users/{id}` | Suppression |
| POST | `/api/v1/users/{id}/lock` | Verrouillage du compte |
| POST | `/api/v1/users/{id}/unlock` | Déverrouillage du compte |
| PUT | `/api/v1/users/{id}/profile` | Mise à jour firstName/lastName |
| PATCH | `/api/v1/users/{id}/roles` | Ajout/suppression en lot de rôles |

### DataSeeder

`DataSeeder` est mis à jour pour insérer un utilisateur admin initial lorsque
`appUserRepository.count() == 0`. Il interroge d'abord `roleRepository.findByRoleName("ADMIN")`
; si le rôle n'existe pas, le seed est ignoré pour éviter une erreur de clé étrangère.

---

## Fichiers clés

| Fichier | Rôle |
|---|---|
| `src/main/java/.../validation/password/ValidPassword.java` | Annotation |
| `src/main/java/.../validation/password/PasswordValidator.java` | Implementor |
| `src/main/java/.../validation/match/PasswordsMatch.java` | Annotation de niveau classe |
| `src/main/java/.../validation/match/PasswordsMatchValidator.java` | Vérification multi-champs |
| `src/main/java/.../validation/unique/Unique.java` | Annotation d'unicité générique |
| `src/main/java/.../validation/unique/UniqueValidator.java` | Requête EntityManager |
| `src/main/java/.../service/UserService.java` | Interface de service utilisateur |
| `src/main/java/.../service/impl/UserServiceImpl.java` | Logique complète de gestion |
| `src/main/java/.../controller/UserController.java` | Endpoints réservés ADMIN |
| `src/main/resources/db/migration/V7__add_role_user_join_table.sql` | Table de jointure + seed |
| `src/main/resources/db/migration/V8__add_account_locked.sql` | Colonne account_locked |
| `src/main/resources/db/migration/V9__add_name_fields_to_app_users.sql` | Colonnes prénom/nom |
