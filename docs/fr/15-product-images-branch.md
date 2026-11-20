# Branche : feature/product-images

Cette branche ajoute la gestion d'images pour les produits. Elle introduit une entité
`ProductImage` dédiée, un service de stockage local de fichiers, et deux nouveaux endpoints
pour téléverser et supprimer des images. Elle illustre `@ConfigurationProperties` pour la
configuration du stockage et la gestion de fichiers multipart dans un contrôleur REST Spring.

---

## Ce que vous allez apprendre

- Comment centraliser la configuration des téléversements avec `@ConfigurationProperties`
- Comment valider le type de fichier (extension + MIME), la taille et le chemin avant l'enregistrement
- Comment appliquer le pattern contrat/implémentation à une préoccupation transversale (le stockage)
- Comment gérer une relation one-to-many où un seul enfant peut être "primaire"
- Comment n'enrichir la réponse avec la liste `images` que sur l'endpoint de détail, pas sur la liste paginée

---

## Ce que cette branche apporte

### Migration Flyway

| Migration | Ce qu'elle fait |
|---|---|
| V16 | Crée la table `product_images` avec un index unique partiel `WHERE is_primary = TRUE`, garantissant au plus une image primaire par produit au niveau de la base de données |

### StorageProperties

`StorageProperties` est un record `@ConfigurationProperties(prefix = "app.storage")` qui
centralise la configuration des téléversements : répertoire d'upload, extensions autorisées,
types MIME autorisés et taille maximale en Mo. Déclaré comme `@Bean` dans `StorageConfig`.

### FileStorageService / LocalFileStorageService

`FileStorageService` est une interface avec une unique méthode `store(MultipartFile, Long productId)`
qui retourne le nom de fichier stocké. `LocalFileStorageService` l'implémente avec :

- Génération de nom de fichier UUID pour éviter les collisions et ignorer le nom original
- Validation de l'extension et du type MIME par rapport aux listes de configuration
- Contrôle de la taille maximale
- Garde contre le path traversal : le chemin absolu résolu est vérifié pour qu'il commence
  bien par le répertoire d'upload configuré avant l'écriture

### ProductImage / ProductImageRepository

`ProductImage` est une entité JPA avec `@ManyToOne` vers `Product`, `filename`,
`originalFilename`, `contentType`, `fileSize`, et `boolean primary` (pas `isPrimary` - voir
Décisions techniques). `ProductImageRepository` fournit `findAllByProductId` et
`deleteByFilenameAndProductId`.

### ProductImageService / ProductImageServiceImpl

`ProductImageServiceImpl` délègue la persistance des fichiers à `FileStorageService` et la
persistance des entités à `ProductImageRepository`. Il gère le "primary swap" : quand une
nouvelle image est téléversée comme primaire, l'image primaire existante (si elle existe) est
rétrogradée pour ne pas violer l'index unique partiel. Il vérifie également la propriété lors
de la suppression : l'image doit appartenir au `productId` donné.

### Extensions de ProductController

Deux nouveaux endpoints sont ajoutés à `ProductController` :

| Méthode | URL | Accès | Description |
|---|---|---|---|
| POST | `/api/v1/products/{id}/images` | ROLE_ADMIN | Téléverser une image (multipart/form-data, champ `file`) |
| DELETE | `/api/v1/products/{id}/images/{imageId}` | ROLE_ADMIN | Supprimer une image |

`ProductResponse` gagne un champ `images` (`List<ProductImageResponse>`). Il est peuplé
uniquement sur `GET /api/v1/products/{id}` (via `ProductMapper.toDetailResponse`) et retourne
une liste vide sur `GET /api/v1/products` (liste paginée) pour éviter le N+1.

---

## Décisions techniques

**`boolean primary`, pas `isPrimary`**
Lombok génère `isPrimary()` comme getter pour un champ nommé `isPrimary`, que MapStruct
normalise en propriété `primary` (en supprimant le préfixe `is`). Nommer le champ `isPrimary`
créait un mismatch entre le nom du champ et le nom de propriété cible de MapStruct. Renommer
le champ en `primary` résout ce mismatch. La colonne SQL reste `is_primary`.

**V16, pas V15**
V14 et V15 sont réservées à `feature/orders` sur une branche parallèle. Utiliser V16 évite
un conflit de checksum Flyway lors de la fusion des deux branches.

**`ProductMapper.toDetailResponse` comme méthode `default`**
Le mapping multi-source MapStruct ne peut pas cibler un constructeur de `record` car les
records sont immuables. `toDetailResponse` est donc une méthode `default` qui appelle
`toResponse` pour construire le DTO de base puis reconstruit le record complet avec la liste
`images` fournie par l'appelant.

---

## Fichiers clés

| Fichier | Rôle |
|---|---|
| `src/main/java/.../config/StorageProperties.java` | `@ConfigurationProperties(prefix="app.storage")` |
| `src/main/java/.../config/StorageConfig.java` | Enregistre `StorageProperties` comme bean |
| `src/main/java/.../service/FileStorageService.java` | Contrat de stockage |
| `src/main/java/.../service/impl/LocalFileStorageService.java` | Implémentation système de fichiers local |
| `src/main/java/.../entity/ProductImage.java` | Entité JPA |
| `src/main/java/.../repository/ProductImageRepository.java` | Repository Spring Data |
| `src/main/java/.../dto/product/ProductImageResponse.java` | DTO image |
| `src/main/java/.../mapper/ProductImageMapper.java` | Mapper MapStruct |
| `src/main/java/.../service/ProductImageService.java` | Contrat de service image |
| `src/main/java/.../service/impl/ProductImageServiceImpl.java` | Logique primary-swap, vérification propriété |
| `src/main/resources/db/migration/V16__add_product_images.sql` | Migration de schéma |
