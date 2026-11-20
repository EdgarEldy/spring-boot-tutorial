# Branch: feature/product-images

This branch adds file-based image management for products. It introduces a dedicated
`ProductImage` entity, a local file storage service, and two new endpoints for uploading and
deleting images. It demonstrates `@ConfigurationProperties` for storage settings and
multipart file handling in a Spring REST controller.

---

## What you will learn

- How to store configuration for file uploads with `@ConfigurationProperties`
- How to validate file type (extension + MIME), size, and path before saving
- How to implement the service contract pattern for a cross-cutting concern (storage)
- How to handle a one-to-many relationship where only one child can be "primary"
- How to serve enriched responses (`images` list) only on the detail endpoint, not the list

---

## What it delivers

### Flyway migration

| Migration | What it does |
|---|---|
| V16 | Creates `product_images` table with a partial unique index `WHERE is_primary = TRUE`, ensuring at most one primary image per product at the database level |

### StorageProperties

`StorageProperties` is a `@ConfigurationProperties(prefix = "app.storage")` record that
centralizes upload configuration: upload directory, allowed extensions, allowed MIME types,
and max file size in MB. Declared as a `@Bean` in `StorageConfig`.

### FileStorageService / LocalFileStorageService

`FileStorageService` is an interface with a single `store(MultipartFile, Long productId)`
method that returns the stored filename. `LocalFileStorageService` implements it with:

- UUID-based filename generation to avoid collisions and bypass the original filename
- Extension and MIME type validation against the configured allow-lists
- Max size enforcement
- Path traversal guard: the resolved absolute path is checked to start with the configured
  upload directory root before writing

### ProductImage / ProductImageRepository

`ProductImage` is a JPA entity with `@ManyToOne` back to `Product`, `filename`, `originalFilename`,
`contentType`, `fileSize`, and `boolean primary` (not `isPrimary` - see Key decisions below).
`ProductImageRepository` provides `findAllByProductId` and `deleteByFilenameAndProductId`.

### ProductImageService / ProductImageServiceImpl

`ProductImageServiceImpl` delegates file persistence to `FileStorageService` and entity
persistence to `ProductImageRepository`. It handles the "primary swap": when a new image is
uploaded as primary, the existing primary image (if any) is demoted first to avoid violating
the partial unique index. It also enforces ownership on delete: the image must belong to the
given product id.

### ProductController extensions

Two new endpoints are added to `ProductController`:

| Method | URL | Access | Description |
|---|---|---|---|
| POST | `/api/v1/products/{id}/images` | ROLE_ADMIN | Upload an image for a product (multipart/form-data, field `file`) |
| DELETE | `/api/v1/products/{id}/images/{imageId}` | ROLE_ADMIN | Delete a product image |

`ProductResponse` gains an `images` field (`List<ProductImageResponse>`). It is populated
only on `GET /api/v1/products/{id}` (via `ProductMapper.toDetailResponse`), and returns an
empty list on `GET /api/v1/products` (the paginated list) to avoid N+1.

---

## Key decisions

**`boolean primary`, not `isPrimary`**
Lombok generates `isPrimary()` as the getter for a field named `isPrimary`, which MapStruct
normalises to the property name `primary` (stripping the `is` prefix). Naming the field
`isPrimary` created a mismatch between the field name and the MapStruct target property name.
Renaming the field to `primary` resolves the mismatch. The SQL column is still `is_primary`.

**V16, not V15**
V14 and V15 are reserved for `feature/orders` on a parallel branch. Using V16 avoids a
Flyway checksum conflict when both branches are eventually merged.

**`ProductMapper.toDetailResponse` as a `default` method**
MapStruct multi-source mapping cannot target a `record` constructor because records are
immutable. `toDetailResponse` is instead a `default` method that calls `toResponse` to build
the base DTO and then reconstructs the full record with the `images` list provided by the caller.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../config/StorageProperties.java` | `@ConfigurationProperties(prefix="app.storage")` |
| `src/main/java/.../config/StorageConfig.java` | Registers `StorageProperties` as a bean |
| `src/main/java/.../service/FileStorageService.java` | Storage contract |
| `src/main/java/.../service/impl/LocalFileStorageService.java` | Local filesystem implementation |
| `src/main/java/.../entity/ProductImage.java` | JPA entity |
| `src/main/java/.../repository/ProductImageRepository.java` | Spring Data repository |
| `src/main/java/.../dto/product/ProductImageResponse.java` | Image DTO |
| `src/main/java/.../mapper/ProductImageMapper.java` | MapStruct mapper |
| `src/main/java/.../service/ProductImageService.java` | Image service contract |
| `src/main/java/.../service/impl/ProductImageServiceImpl.java` | Primary-swap logic, ownership check |
| `src/main/resources/db/migration/V16__add_product_images.sql` | Schema migration |
