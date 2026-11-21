# Phase 1 — feature/products (catalogue e-commerce)

> **Status : TERMINÉE** — 15 commits appliqués (V13 migration, BigDecimal, description, active).

Cette commande est conservée comme documentation de référence.

## Ce qui a été fait

- Migration `V13__add_product_description_active_decimal_price.sql` : champ `description VARCHAR(500)`, champ `active BOOLEAN NOT NULL DEFAULT TRUE`, conversion `unit_price REAL` → `NUMERIC(12,2)`
- `Product.java` : `float` → `BigDecimal`, nouveaux champs `description` et `active`
- `ProductRequest.java` : `Float` → `BigDecimal`, champ `description` optionnel
- `ProductResponse.java` : `float` → `BigDecimal`, champs `description` et `active`
- `ProductMapper.java` : `@Mapping(target="active", ignore=true)` sur les méthodes d'écriture
- `ProductSpecifications.java` : `Float` → `BigDecimal` dans `priceBetween`
- `ProductService.java` / `ProductServiceImpl.java` / `ProductController.java` : même remplacement de type
- 5 fichiers de tests mis à jour
- `README.md` mis à jour

## Branche

`feature/products` — HEAD : `dcb8a46`
