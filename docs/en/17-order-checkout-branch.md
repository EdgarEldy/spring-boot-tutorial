# Branch: feature/order-checkout

This branch replaces the manual order-creation endpoint with an atomic checkout operation
that converts the authenticated user's cart into an order. It demonstrates transactional
integrity, price re-read as a security measure, and the use of JPA `orphanRemoval` to clear
a child collection safely inside a managed transaction.

---

## What you will learn

- How to implement an atomic "convert cart to order" transaction in a Spring service
- Why prices must be re-read from the database at checkout time, not trusted from the cart
- How to clear a JPA collection with `orphanRemoval` correctly vs. a bulk-delete repository call
- How to link an order to the authenticated user via `@AuthenticationPrincipal` without putting the user id in the request body

---

## What it delivers

### Flyway migration

| Migration | What it does |
|---|---|
| V18 | Adds a nullable `user_id` column to `orders` with a FK to `app_users`. Nullable because legacy CRUD orders (pre-checkout) have no user. |

### Order entity update

`Order` gains `@ManyToOne(fetch = LAZY) AppUser user`. The column is nullable; it is set only
for checkout orders, never for legacy CRUD orders.

### Checkout flow

`OrderServiceImpl.checkout(String username)`:

1. Loads the authenticated user's cart (throws 404 if the cart is empty).
2. For each `CartItem`, re-reads the product from the database via `productRepository.findByIdWithCategory` to get the current price. This protects against price manipulation via a stale cart.
3. Creates one `OrderItem` per cart item using the product snapshot (name + current unit price).
4. Saves the `Order` with all items in a single `@Transactional` call.
5. Clears the cart by calling `cart.getItems().clear()`. Because `Cart.items` has `orphanRemoval = true`, Hibernate deletes the orphaned `CartItem` rows on flush, inside the same transaction. See Key decisions below.

### OrderController change

`create(@RequestBody OrderRequest)` is replaced with `checkout(@AuthenticationPrincipal UserDetails)`.
The endpoint is `POST /api/v1/orders` with no request body. The user identity comes from the
JWT, not from the client.

`SecurityConfig` is updated: `POST /api/v1/orders` changes from `hasRole("ADMIN")` to
`hasRole("USER")`.

---

## Key decisions

**Prices re-read from the database at checkout**
A `CartItem` stores the quantity and a reference to a `Product`. It does not store the price.
At checkout, the product is reloaded with `findByIdWithCategory` to get the current
`unitPrice` from the database. This prevents a client from manipulating prices by modifying
a cart item that was added when the price was lower. Test `_checkout_06_ShouldUsePriceFromDB_NotFromCachedCartItem` covers this invariant.

**`cart.getItems().clear()` instead of `cartItemRepository.deleteAllByCartId`**
Inside a `@Transactional` method where the `Cart` entity is managed by Hibernate, calling
`cartItemRepository.deleteAllByCartId()` issues a `DELETE` SQL statement. However, because
`Cart` is still managed with its `items` collection in memory, Hibernate's cascade `PERSIST`
re-inserts the just-deleted items on flush. The correct approach is `cart.getItems().clear()`:
Hibernate sees the orphans and schedules their deletion via `orphanRemoval`, within the same
flush cycle, without the re-insert. This bug was discovered and fixed in `feature/integration`.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../service/OrderService.java` | Adds `checkout(String username)` |
| `src/main/java/.../service/impl/OrderServiceImpl.java` | Atomic checkout implementation |
| `src/main/java/.../controller/OrderController.java` | Replaces create with checkout |
| `src/main/resources/db/migration/V18__add_user_id_to_orders.sql` | Nullable user FK |
