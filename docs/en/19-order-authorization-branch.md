# Branch: feature/order-authorization

This branch makes the order resource role-aware: regular users can only see and act on their
own orders, while admins have full visibility. It also adds a lifecycle-enforced status
transition system and covers it with a true end-to-end Testcontainers test.

---

## What you will learn

- How to implement role-aware list and detail endpoints in a service layer
- How to enforce a status transition matrix and return 422 (Unprocessable Entity) for invalid transitions
- How to combine `@PreAuthorize` and `@AuthenticationPrincipal` on a controller method
- How to write a multi-actor end-to-end test with Testcontainers (userA, userB, admin)
- Why `INNER JOIN` on nullable FK columns silently drops rows, and when to use `LEFT JOIN FETCH` instead

---

## What it delivers

### Role-aware order access

`OrderService` gains two new signatures:

- `findAll(String username, boolean isAdmin, Pageable)` - admins receive all orders; regular
  users receive only orders where `order.user.username = username` (via `user_id` FK).
- `findById(Long id, String username, boolean isAdmin)` - admins can retrieve any order; a
  regular user gets a 404 if the order belongs to another user.

### Order status lifecycle

`updateStatus(Long id, OrderStatus newStatus)` enforces the following transition matrix:

| From | Allowed transitions |
|---|---|
| PENDING | CONFIRMED, CANCELLED |
| CONFIRMED | CANCELLED |
| CANCELLED | (none) |

Any other transition returns 422 `Unprocessable Entity`.

### OrderController rewrite

`OrderController` is rewritten with `@AuthenticationPrincipal` and `@PreAuthorize`:

- `GET /api/v1/orders` - `@PreAuthorize("isAuthenticated()")`, delegates with username + isAdmin
- `GET /api/v1/orders/{id}` - `@PreAuthorize("isAuthenticated()")`, delegates with username + isAdmin
- `PATCH /api/v1/orders/{id}/status` - `@PreAuthorize("hasRole('ADMIN')")`, body `OrderStatusUpdateRequest`

### Bug fix: LEFT JOIN FETCH on nullable FKs

`findByIdWithDetails` previously used `INNER JOIN` on `customer` and `product`. Checkout
orders have `customer_id = NULL` and `product_id = NULL`. An `INNER JOIN` on a nullable
column silently returns no rows when the value is null, so checkout orders were invisible to
`findById`. Fixed by switching to `LEFT JOIN FETCH` on those two associations.

### OrderAuthorizationTest

8 Testcontainers integration test cases covering:

- userA can see their own order
- userA gets 404 on userB's order
- admin can see all orders (findAll and findById)
- admin can update status (PENDING to CONFIRMED)
- regular user cannot update status (403)
- invalid status transition returns 422

---

## Key decisions

**404 instead of 403 for cross-user order access**
Returning 403 (Forbidden) would reveal that an order with the given id exists, which is
itself information. Returning 404 (Not Found) for a user trying to access another user's
order prevents enumeration of other users' order ids.

**`findAllByCustomerId` added to `OrderService`**
`CustomerController.findOrders` previously called `orderService.findAll` directly. With the
new role-aware signature, `findAllByCustomerId(Long, Pageable)` is added as a dedicated
method for the nested endpoint, decoupled from the user-scoped `findAll`.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../dto/order/OrderStatusUpdateRequest.java` | PATCH status request DTO |
| `src/main/java/.../service/OrderService.java` | Role-aware findAll/findById + updateStatus |
| `src/main/java/.../service/impl/OrderServiceImpl.java` | Transition matrix, ownership check |
| `src/main/java/.../controller/OrderController.java` | @PreAuthorize + @AuthenticationPrincipal |
| `src/test/java/.../OrderAuthorizationTest.java` | 8-case Testcontainers e2e test |
