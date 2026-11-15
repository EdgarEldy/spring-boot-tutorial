# API REST

Ce document couvre la stratégie de versioning, les formats de réponse standard, les
patterns CRUD et la façon d'explorer l'API interactivement avec Swagger UI.

---

## Versioning

Tous les endpoints sont préfixés par `/api/v1` :

```
GET  /api/v1/categories
POST /api/v1/products
GET  /api/v1/orders/42
```

Le préfixe est défini sur le `@RequestMapping` de chaque `@RestController`. Le placer au
niveau de la classe plutôt qu'au niveau de la méthode garantit qu'aucun endpoint n'est
accidentellement laissé sans version.

> **Pourquoi versionner dès le départ ?** Une fois l'API en production, changer les chemins
> casse les clients. Mettre `/api/v1` en place avant la première branche fonctionnelle signifie
> que le versioning est une fondation, pas un ajout tardif. Lorsqu'un changement incompatible
> est nécessaire, un préfixe `/api/v2` peut être introduit pour des ressources spécifiques
> sans toucher à v1.

---

## Format de réponse

**Toute réponse** - succès ou erreur - est encapsulée dans `ApiResponse<T>` :

```json
{
  "success": true,
  "message": "Category created successfully",
  "data": { "id": 1, "name": "Electronics" },
  "timestamp": "2025-11-15T10:30:00"
}
```

**Les listes paginées** utilisent `PageResponse<T>` comme champ `data` :

```json
{
  "success": true,
  "message": "Categories retrieved successfully",
  "data": {
    "content": [ { "id": 1, "name": "Electronics" } ],
    "page": 0,
    "size": 10,
    "total_elements": 1,
    "total_pages": 1,
    "last": true
  }
}
```

Les codes de statut HTTP sont définis explicitement via `ResponseEntity`. Le corps JSON a
toujours la même structure quel que soit le code de statut, ce qui simplifie la gestion des
erreurs côté client.

---

## Patterns CRUD

| Méthode HTTP | Pattern de chemin | Statut en cas de succès | Type de réponse |
|---|---|---|---|
| GET | `/api/v1/resources?page=0&size=10` | 200 | `ApiResponse<PageResponse<XxxResponse>>` |
| GET | `/api/v1/resources/{id}` | 200 | `ApiResponse<XxxResponse>` |
| POST | `/api/v1/resources` | 201 | `ApiResponse<XxxResponse>` |
| PUT | `/api/v1/resources/{id}` | 200 | `ApiResponse<XxxResponse>` |
| DELETE | `/api/v1/resources/{id}` | 200 | `ApiResponse<Void>` |

Les paramètres de pagination ont des valeurs par défaut de `page=0` et `size=10`. Le tri est
supporté via `sort=field,asc` ou `sort=field,desc` sur la plupart des endpoints de liste.

---

## Vue d'ensemble des endpoints

| Ressource | Endpoints |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `GET /auth/me`, `POST /auth/logout`, `GET /auth/activate-account`, `POST /auth/forgot-password`, `POST /auth/reset-password` |
| Catégories | `GET/POST /api/v1/categories`, `GET/PUT/DELETE /api/v1/categories/{id}` |
| Produits | `GET/POST /api/v1/products`, `GET/PUT/DELETE /api/v1/products/{id}` |
| Clients | `GET/POST /api/v1/customers`, `GET/PUT/DELETE /api/v1/customers/{id}`, `GET /api/v1/customers/{id}/orders` |
| Commandes | `GET/POST /api/v1/orders`, `GET/PUT/DELETE /api/v1/orders/{id}` |
| Utilisateurs | `GET /api/v1/users`, `GET/DELETE /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/roles`, `POST /api/v1/users/{id}/lock`, `POST /api/v1/users/{id}/unlock`, `PUT /api/v1/users/{id}/profile` |
| Rôles | `GET/POST /api/v1/roles`, `GET/PUT/DELETE /api/v1/roles/{id}`, `PATCH /api/v1/roles/{id}/permissions` |
| Permissions | `GET/POST /api/v1/permissions`, `GET/DELETE /api/v1/permissions/{id}` |

---

## Swagger UI

Swagger UI est disponible à l'adresse `http://localhost:8080/swagger-ui.html` lorsque
l'application est en cours d'exécution. Il est alimenté par springdoc-openapi 2.8.16 et lit
les annotations `@Operation` sur chaque endpoint.

**Autoriser les requêtes dans Swagger UI :**

1. Appelez `POST /auth/login` et copiez le champ `access_token` depuis la réponse.
2. Cliquez sur **Authorize** en haut à droite de la page Swagger UI.
3. Dans le champ BearerAuth, saisissez le token (sans le préfixe `Bearer ` - Swagger l'ajoute automatiquement).
4. Cliquez sur **Authorize**, puis sur **Close**.

Toutes les requêtes suivantes incluront automatiquement `Authorization: Bearer <token>`.

> La définition OpenAPI est également disponible en JSON à l'adresse
> `http://localhost:8080/v3/api-docs`.
