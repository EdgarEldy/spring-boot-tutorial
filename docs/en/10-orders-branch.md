# Branch: feature/orders

This branch adds the Order resource and introduces Spring's application event system. An
order ties a customer to a product; creating one is the most complex write operation in the
application because it touches two other resources and publishes a domain event.

---

## What you will learn

- How to publish a domain event from inside a transaction using Spring's `ApplicationEventPublisher`
- Why `@TransactionalEventListener(phase = AFTER_COMMIT)` is safer than `@EventListener` for post-commit side effects
- How to avoid N+1 queries on list endpoints with JPQL projections (`SELECT new ClassName(...)`)
- How to load a full object graph for detail endpoints with `JOIN FETCH`
- How to build a nested REST endpoint (`/customers/{id}/orders`)
- How to add delete guards on multiple related resources

---

## What it delivers

### Order entity

`Order` has a `@ManyToOne` relationship to both `Customer` and `Product`, both `NOT NULL`.
It also stores `quantity` and `totalPrice` (denormalised at create time).

### OrderCreatedEvent and OrderCreatedEventListener

`OrderServiceImpl.create()` is annotated `@Transactional`. After saving the order, it calls
`applicationEventPublisher.publishEvent(new OrderCreatedEvent(savedOrder))`.

`OrderCreatedEventListener` handles the event with
`@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.

> **Why AFTER_COMMIT?** `@EventListener` fires synchronously inside the current transaction.
> If the listener throws an exception or if the transaction rolls back after the listener
> runs, the event was already processed for a row that no longer exists. `AFTER_COMMIT`
> guarantees the listener fires only after the database row is durable. If the transaction
> rolls back, the listener never runs.

### JPQL projection for the list endpoint

`OrderRepository.findAllProjected(Pageable)` uses a JPQL `SELECT new` query that selects
exactly the columns needed for `OrderProjection`. This loads all data for a full page of
orders in a single SQL query, with no secondary selects for the related customer or product.

### JOIN FETCH for the detail endpoint

`OrderRepository.findByIdWithDetails(Long id)` uses `JOIN FETCH` to load the customer and
product in a single query for the detail view. This is appropriate for a single-row lookup
where the full object graph is always needed.

### Delete guards

`CustomerServiceImpl.delete()` and `ProductServiceImpl.delete()` both check
`OrderRepository.existsByCustomerId(id)` and `OrderRepository.existsByProductId(id)` before
removing the entity. This prevents foreign key violations on `orders.customer_id` and
`orders.product_id` from reaching the client as 500 errors.

### Nested endpoint

`CustomerController` exposes `GET /api/v1/customers/{id}/orders`. It first verifies the
customer exists (calling `CustomerService.findById`, raising 404 if absent), then delegates
to `OrderService.findAll` filtered by customer id. The logic stays at the controller level
because it orchestrates two services; no dedicated service method was added for this case.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../entity/Order.java` | Order JPA entity |
| `src/main/java/.../event/OrderCreatedEvent.java` | Domain event record |
| `src/main/java/.../event/OrderCreatedEventListener.java` | AFTER_COMMIT listener |
| `src/main/java/.../repository/OrderRepository.java` | Projection + JOIN FETCH queries |
| `src/main/java/.../service/OrderService.java` | Service interface |
| `src/main/java/.../service/impl/OrderServiceImpl.java` | @Transactional, publishes event |
| `src/main/java/.../controller/OrderController.java` | REST endpoints |
| `src/main/java/.../controller/CustomerController.java` | Nested /orders endpoint |
