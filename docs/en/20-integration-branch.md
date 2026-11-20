# Branch: feature/integration

This branch merges `feature/order-authorization` and `feature/product-images` into a single
integration branch, fixes a critical cart-clearing bug discovered only at integration time,
and validates the full e-commerce flow with an end-to-end test.

---

## What you will learn

- How a cascade `PERSIST` bug can silently undo a correct-looking `DELETE` inside a transaction
- Why an `orphanRemoval` clear is the correct way to delete JPA children in a managed session
- How to write a multi-step end-to-end test that covers an entire user journey
- Why integration bugs often appear only when two independently-tested branches are merged

---

## What it delivers

### Critical bug fix: cart not cleared after checkout

After merging `feature/order-checkout` (which introduced `checkout()`) with the full
e-commerce stack, a critical regression appeared: the cart was not emptied after a successful
checkout.

**Root cause:** `checkout()` originally called
`cartItemRepository.deleteAllByCartId(cart.getId())`. Inside the same `@Transactional`
method, the `Cart` entity is managed by Hibernate with its `items` collection still in
memory. After the `DELETE` SQL is issued, Hibernate's cascade `PERSIST` re-inserts the
just-deleted items on flush because the managed collection still references them.

**Fix:** `cart.getItems().clear()` replaces the repository call. With
`orphanRemoval = true` on `Cart.items`, Hibernate schedules the deletion of orphaned
`CartItem` rows during the flush phase, within the same transaction, without the re-insert.
`CartItemRepository` is removed from `OrderServiceImpl` as a consequence.

### ProductResponse constructor extension

`feature/product-images` added an `images` field (8th argument) to the `ProductResponse`
record constructor. After the merge, every call site that constructed a `ProductResponse`
with 7 arguments broke. Fixed in:

- `OrderMapper.toResponse(OrderProjection)` - the projection-based list path
- `CustomerControllerTest` - sample customer construction
- `OrderControllerTest` - `sampleResponse()` helper
- `OrderServiceImplTest` - all `OrderResponse` assertions

### EcommerceIntegrationTest

A single `@SpringBootTest` Testcontainers test that walks through 11 steps:

1. Register userA
2. Login userA, obtain JWT
3. Verify product catalogue is accessible without authentication
4. Add two different products to cart
5. GET cart, verify items and subtotals
6. PATCH one item quantity
7. POST checkout - order created, cart emptied
8. GET orders (userA) - sees one order
9. GET order by id - detail with items, status = PENDING
10. Register admin + login admin
11. PATCH order status to CONFIRMED - 200; GET order as userA - status = CONFIRMED

---

## Key decisions

**`cart.getItems().clear()` - the idiomatic JPA approach**
`deleteAllByCartId()` bypasses the Hibernate session. In a session where `Cart` is managed
with its collection, the ORM's first-level cache and dirty-checking will undo any direct SQL
delete if the in-memory state still holds references. The idiomatic fix is to modify the
collection through the managed object and let the session propagate the change. This is a
fundamental JPA concept that only surfaces when entity graphs with `cascade` and
`orphanRemoval` are exercised in the same transaction as a child collection mutation.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../service/impl/OrderServiceImpl.java` | Bug fix: `cart.getItems().clear()` |
| `src/main/java/.../mapper/OrderMapper.java` | 8-arg `ProductResponse` constructor |
| `src/test/java/.../EcommerceIntegrationTest.java` | 11-step e2e test |
