# API Reference

Base URL: `http://localhost:8080/api/v1`

Interactive documentation is also available via Swagger UI at `http://localhost:8080/swagger-ui.html`.

## Authentication

Endpoints that require authentication expect a JWT access token in the `Authorization` header:

```
Authorization: Bearer <access_token>
```

Obtain a token via `POST /auth/login`. Access levels in this document:

| Level | Meaning |
|---|---|
| Public | No token required |
| Authenticated | Any valid, non-expired, non-blacklisted token for an enabled, non-locked account |
| ADMIN | Token must carry the `ROLE_ADMIN` authority |

---

## Standard response format

Every response body follows this envelope:

```json
{
  "success": true,
  "message": "...",
  "data": { ... },
  "timestamp": "2026-01-01T00:00:00Z"
}
```

**Paginated lists** return a `PageResponse` as `data`:

```json
{
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "total_elements": 42,
    "total_pages": 3
  }
}
```

**Errors** return `success: false` and a RFC 7807 `ProblemDetail` as `data`:

```json
{
  "success": false,
  "message": "Product not found",
  "data": {
    "type": "about:blank",
    "title": "Not Found",
    "status": 404,
    "detail": "Product not found with id 99",
    "instance": "/api/v1/products/99"
  },
  "timestamp": "2026-01-01T00:00:00Z"
}
```

Validation errors (400) add a `fieldErrors` map on `data`:

```json
{
  "data": {
    "status": 400,
    "fieldErrors": { "product_name": "must not be blank" }
  }
}
```

---

## Categories

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/categories` | Paginated list | Public |
| GET | `/categories/{id}` | Category detail with associated products | Public |
| POST | `/categories` | Create a category | ADMIN |
| PUT | `/categories/{id}` | Update a category | ADMIN |
| DELETE | `/categories/{id}` | Delete a category (fails with 422 if products exist) | ADMIN |

**Query parameters** (`GET /categories`): `page` (default 0), `size` (default 20), `sort` (e.g. `categoryName,asc`).

**CategoryRequest** (POST / PUT body):
```json
{ "category_name": "Electronics" }
```

**CategoryResponse**:
```json
{
  "id": 1,
  "category_name": "Electronics",
  "created_at": "2026-01-01T00:00:00Z",
  "updated_at": "2026-01-01T00:00:00Z",
  "products": []
}
```
`products` is populated only on the detail endpoint (`GET /categories/{id}`), not on the list.

---

## Products

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/products` | Paginated list, filterable and sortable | Public |
| GET | `/products/{id}` | Product detail | Public |
| POST | `/products` | Create a product | ADMIN |
| PUT | `/products/{id}` | Update a product | ADMIN |
| DELETE | `/products/{id}` | Delete a product (fails with 422 if orders exist) | ADMIN |

**Query parameters** (`GET /products`):

| Parameter | Type | Description |
|---|---|---|
| `categoryId` | Long | Filter by category |
| `productName` | String | Case-insensitive contains search |
| `minPrice` | Double | Minimum unit price |
| `maxPrice` | Double | Maximum unit price |
| `page` | int | Page index (default 0) |
| `size` | int | Page size (default 20) |
| `sort` | String | e.g. `unitPrice,desc` |

**ProductRequest** (POST / PUT body):
```json
{ "product_name": "Mechanical keyboard", "unit_price": 79.99, "category_id": 1 }
```

**ProductResponse**:
```json
{
  "id": 12,
  "product_name": "Mechanical keyboard",
  "unit_price": 79.99,
  "category_id": 1,
  "category_name": "Electronics",
  "version": 0,
  "created_at": "2026-01-01T00:00:00Z",
  "updated_at": "2026-01-01T00:00:00Z"
}
```

`PUT /products/{id}` requires the client to send the current `version` value. A stale version returns 409 Conflict.

---

## Customers

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/customers` | Paginated list, searchable by name | Authenticated |
| GET | `/customers/{id}` | Customer detail with ordered products | Authenticated |
| POST | `/customers` | Create a customer | ADMIN |
| PUT | `/customers/{id}` | Update a customer | ADMIN |
| DELETE | `/customers/{id}` | Delete a customer (fails with 422 if orders exist) | ADMIN |
| GET | `/customers/{id}/orders` | Orders placed by a customer | Authenticated |

**Query parameters** (`GET /customers`): `search` (matches first or last name), `page`, `size`, `sort`.

**CustomerRequest** (POST / PUT body):
```json
{
  "first_name": "Ada", "last_name": "Lovelace",
  "telephone": "+1 202-555-0100",
  "email": "ada@example.com",
  "address": "1 Analytical Engine Way"
}
```

**CustomerResponse**:
```json
{
  "id": 1, "first_name": "Ada", "last_name": "Lovelace",
  "telephone": "+1 202-555-0100",
  "email": "ada@example.com",
  "address": "1 Analytical Engine Way",
  "products": []
}
```
`products` (the distinct products ordered by this customer) is populated only on `GET /customers/{id}`.

---

## Orders

| Method | URL | Description | Access |
|---|---|---|---|
| GET | `/orders` | Paginated list, filterable by customer or product | Authenticated |
| GET | `/orders/{id}` | Order detail | Authenticated |
| POST | `/orders` | Create an order | ADMIN |
| PUT | `/orders/{id}` | Update an order | ADMIN |
| DELETE | `/orders/{id}` | Delete an order | ADMIN |

**Query parameters** (`GET /orders`): `customerId`, `productId`, `page`, `size`, `sort`.

**OrderRequest** (POST / PUT body):
```json
{ "customer_id": 1, "product_id": 12, "quantity": 2 }
```

`total` is computed server-side as `quantity * product.unitPrice`.

**OrderResponse** embeds the full customer and product DTOs:
```json
{
  "id": 5,
  "customer": { "id": 1, "first_name": "Ada", ... },
  "product":  { "id": 12, "product_name": "Mechanical keyboard", ... },
  "quantity": 2,
  "total": 159.98
}
```

---

## Authentication

| Method | URL | Description | Access |
|---|---|---|---|
| POST | `/auth/register` | Create an account (sends activation email) | Public |
| POST | `/auth/login` | Sign in, returns access + refresh tokens | Public |
| POST | `/auth/refresh-token` | Exchange a refresh token for a new access token | Public |
| GET | `/auth/me` | Current user profile | Authenticated |
| POST | `/auth/logout` | Revoke the current access token + refresh token | Authenticated |
| GET | `/auth/activate` | Activate account with email token | Public |
| POST | `/auth/forgot-password` | Request a password-reset email | Public |
| POST | `/auth/reset-password` | Reset password with email token | Public |
| GET | `/auth/token-info` | Inspect claims of the current access token | Authenticated |

**RegisterRequest**:
```json
{
  "first_name": "Ada", "last_name": "Lovelace",
  "username": "ada",
  "email": "ada@example.com",
  "password": "Password1@",
  "confirm_password": "Password1@"
}
```

Password rules: minimum 8 characters, at least one uppercase letter, one digit, one special character (`@#$%^&+=!`), no spaces.

**LoginRequest**:
```json
{ "email": "ada@example.com", "password": "Password1@" }
```

**AuthResponse** (login / refresh):
```json
{
  "access_token": "eyJhbGc...",
  "token_type": "Bearer",
  "refresh_token": "dGhpcyBp...",
  "expires_in": 3600
}
```

`expires_in` is in seconds (default: 3600). The refresh token is valid for 7 days.

**Refresh token** (`POST /auth/refresh-token`): send the refresh token in the `Authorization` header:
```
Authorization: Bearer <refresh_token>
```

**Logout** (`POST /auth/logout`): send the access token in the `Authorization` header. Both the access token (blacklisted by `jti`) and the refresh token are invalidated.

**Activate** (`GET /auth/activate`): `?token=<activation_token_from_email>`

**Forgot password** (`POST /auth/forgot-password`):
```json
{ "email": "ada@example.com" }
```

**Reset password** (`POST /auth/reset-password?token=<reset_token>`):
```json
{ "new_password": "NewPass1@", "confirm_password": "NewPass1@" }
```

---

## Roles

All endpoints are ADMIN-only.

| Method | URL | Description |
|---|---|---|
| GET | `/roles` | Paginated list of roles with their permissions |
| POST | `/roles` | Create a role |
| PUT | `/roles/{id}` | Rename a role |
| DELETE | `/roles/{id}` | Delete a role |
| POST | `/roles/{id}/permissions/{permissionId}` | Assign a permission to a role |
| DELETE | `/roles/{id}/permissions/{permissionId}` | Remove a permission from a role |

**RoleRequest**:
```json
{ "role_name": "MODERATOR" }
```

---

## Permissions

All endpoints are ADMIN-only.

| Method | URL | Description |
|---|---|---|
| GET | `/permissions` | Paginated list of permissions |
| POST | `/permissions` | Create a permission |
| DELETE | `/permissions/{id}` | Delete a permission |

**PermissionRequest**:
```json
{ "resource": "PRODUCTS", "action": "WRITE" }
```

---

## Users

All endpoints are ADMIN-only.

| Method | URL | Description |
|---|---|---|
| GET | `/users` | Paginated list of users |
| GET | `/users/{id}` | User detail |
| PUT | `/users/{id}/roles` | Assign roles to a user |
| DELETE | `/users/{id}/roles/{roleId}` | Remove a role from a user |
| DELETE | `/users/{id}` | Delete a user |
| POST | `/users/{id}/lock` | Lock an account |
| POST | `/users/{id}/unlock` | Unlock an account |
| PUT | `/users/{id}/profile` | Update a user's profile (firstName, lastName) |

**AssignRolesRequest**:
```json
{ "role_ids": [1, 2] }
```

---

## Common HTTP status codes

| Code | When |
|---|---|
| 200 OK | Successful read or update |
| 201 Created | Successful creation |
| 400 Bad Request | Validation failure, invalid token |
| 401 Unauthorized | Missing, expired, or blacklisted token; bad credentials; account disabled |
| 403 Forbidden | Authenticated but insufficient role or permission |
| 404 Not Found | Resource does not exist |
| 409 Conflict | Optimistic locking failure (stale `version` on a product update) |
| 422 Unprocessable Entity | Business rule violation (e.g. delete a category that still has products) |
| 500 Internal Server Error | Unexpected server error |
