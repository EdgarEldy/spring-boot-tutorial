# REST API

This document covers the versioning strategy, standard response shapes, CRUD patterns, and
how to explore the API interactively with Swagger UI.

---

## Versioning

All endpoints are prefixed with `/api/v1`:

```
GET  /api/v1/categories
POST /api/v1/products
GET  /api/v1/orders/42
```

The prefix is set on the `@RequestMapping` of each `@RestController`. Adding it at the class
level rather than the method level ensures no endpoint is accidentally left unversioned.

> **Why version from the start?** Once an API is in use, changing paths breaks clients.
> Putting `/api/v1` in place before the first feature branch means versioning is a foundation,
> not a retrofit. When a breaking change is needed, a `/api/v2` prefix can be introduced for
> specific resources without touching v1.

---

## Response format

**Every response** - success or error - is wrapped in `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Category created successfully",
  "data": { "id": 1, "name": "Electronics" },
  "timestamp": "2025-11-15T10:30:00"
}
```

**Paginated lists** use `PageResponse<T>` as the `data` field:

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

HTTP status codes are set explicitly via `ResponseEntity`. The JSON body always has the same
shape regardless of status code, which simplifies client-side error handling.

---

## CRUD patterns

| HTTP Method | Path pattern | Status on success | Response type |
|---|---|---|---|
| GET | `/api/v1/resources?page=0&size=10` | 200 | `ApiResponse<PageResponse<XxxResponse>>` |
| GET | `/api/v1/resources/{id}` | 200 | `ApiResponse<XxxResponse>` |
| POST | `/api/v1/resources` | 201 | `ApiResponse<XxxResponse>` |
| PUT | `/api/v1/resources/{id}` | 200 | `ApiResponse<XxxResponse>` |
| DELETE | `/api/v1/resources/{id}` | 200 | `ApiResponse<Void>` |

Pagination parameters default to `page=0` and `size=10`. Sorting is supported via
`sort=field,asc` or `sort=field,desc` on most list endpoints.

---

## Endpoint overview

| Resource | Endpoints |
|---|---|
| Auth | `POST /auth/register`, `POST /auth/login`, `GET /auth/me`, `POST /auth/logout`, `GET /auth/activate-account`, `POST /auth/forgot-password`, `POST /auth/reset-password` |
| Categories | `GET/POST /api/v1/categories`, `GET/PUT/DELETE /api/v1/categories/{id}` |
| Products | `GET/POST /api/v1/products`, `GET/PUT/DELETE /api/v1/products/{id}` |
| Customers | `GET/POST /api/v1/customers`, `GET/PUT/DELETE /api/v1/customers/{id}`, `GET /api/v1/customers/{id}/orders` |
| Orders | `GET/POST /api/v1/orders`, `GET/PUT/DELETE /api/v1/orders/{id}` |
| Users | `GET /api/v1/users`, `GET/DELETE /api/v1/users/{id}`, `PATCH /api/v1/users/{id}/roles`, `POST /api/v1/users/{id}/lock`, `POST /api/v1/users/{id}/unlock`, `PUT /api/v1/users/{id}/profile` |
| Roles | `GET/POST /api/v1/roles`, `GET/PUT/DELETE /api/v1/roles/{id}`, `PATCH /api/v1/roles/{id}/permissions` |
| Permissions | `GET/POST /api/v1/permissions`, `GET/DELETE /api/v1/permissions/{id}` |

---

## Swagger UI

Swagger UI is available at `http://localhost:8080/swagger-ui.html` when the application is
running. It is powered by springdoc-openapi 2.8.16 and reads `@Operation` annotations on
every endpoint.

**Authorizing requests in Swagger UI:**

1. Call `POST /auth/login` and copy the `access_token` from the response.
2. Click **Authorize** at the top right of the Swagger UI page.
3. In the BearerAuth field, enter the token (without the `Bearer ` prefix - Swagger adds it).
4. Click **Authorize**, then **Close**.

All subsequent requests will include `Authorization: Bearer <token>` automatically.

> The OpenAPI definition is also available as JSON at
> `http://localhost:8080/v3/api-docs`.
