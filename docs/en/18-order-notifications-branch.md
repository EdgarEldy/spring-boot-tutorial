# Branch: feature/order-notifications

This branch adds asynchronous email notifications for checkout orders. It shows how to wire
`@Async` to a named executor, how to reload a full aggregate after the transaction commits,
and why email failures must never propagate to the caller.

---

## What you will learn

- How to send email with `JavaMailSender` and `SimpleMailMessage`
- Why email logic must run on a dedicated thread pool (`@Async`) to avoid blocking the HTTP thread
- Why mail exceptions must be caught and logged, never rethrown
- How to reload a full entity graph after `AFTER_COMMIT` to get the user and items in context
- How to test async listeners synchronously by stubbing the dependencies rather than the executor

---

## What it delivers

### NotificationProperties

`NotificationProperties` is a `@ConfigurationProperties(prefix = "app.notifications")` record
with a single `adminEmail` field. It is registered in `AsyncConfig` alongside the
`emailTaskExecutor` bean.

### OrderEmailService / OrderEmailServiceImpl

`OrderEmailService` defines two methods: `sendOrderConfirmation(Order, AppUser)` for the
customer and `sendAdminNotification(Order)` for the administrator.

`OrderEmailServiceImpl` is annotated with `@Async("emailTaskExecutor")`. Each method:

- Builds a `SimpleMailMessage` with plain-text content
- Calls `javaMailSender.send(message)`
- Wraps the call in a `try/catch(MailException)` and logs the exception at ERROR level if
  sending fails. The exception is never rethrown. An email failure must not roll back the
  order that was already committed.

### OrderRepository extension

`findByIdWithItemsAndUser` is added with a JPQL `LEFT JOIN FETCH` on both `items` and `user`.
A new method is used instead of modifying the existing `findByIdWithDetails` because the
existing query uses `INNER JOIN` on `customer` and `product` (legacy CRUD orders), which
would return `Optional.empty()` for checkout orders that have no `customer_id` or `product_id`.

### OrderCreatedEventListener

`OrderCreatedEventListener` is enriched to:

1. Run on `emailTaskExecutor` via `@Async("emailTaskExecutor")`.
2. Reload the full order from the database using `findByIdWithItemsAndUser`. The event carries only the order id; the full object is not passed through the event to avoid stale data issues after the transaction commits.
3. Call `orderEmailService.sendOrderConfirmation` if the order has a user, and `orderEmailService.sendAdminNotification` unconditionally.

---

## Key decisions

**Separate `findByIdWithItemsAndUser` vs modifying `findByIdWithDetails`**
`findByIdWithDetails` uses `INNER JOIN` on `customer` and `product`. Checkout orders have
`customer_id = NULL` and `product_id = NULL`, so an `INNER JOIN` on those columns returns no
rows. Modifying the existing method would break `OrderServiceImpl.findById` and all its tests.
Adding a new method keeps the two read paths independent and allows both to coexist.

**`@Async` on the event listener**
`@TransactionalEventListener(phase = AFTER_COMMIT)` already guarantees that the listener runs
after the order transaction is committed. Adding `@Async("emailTaskExecutor")` moves the
execution off the HTTP request thread entirely, so the client response (201 Created) is not
delayed by SMTP latency.

**Stubbing per test method, not in `@BeforeEach`**
Mockito strict stubs (`STRICT_STUBS` mode) throw `UnnecessaryStubbingException` for stubs
that are set up but never consumed in a given test. `notificationProperties.adminEmail()` is
stubbed only inside the test methods that actually invoke it, not in `@BeforeEach`.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../config/NotificationProperties.java` | `@ConfigurationProperties(prefix="app.notifications")` |
| `src/main/java/.../service/OrderEmailService.java` | Email service contract |
| `src/main/java/.../service/impl/OrderEmailServiceImpl.java` | Async, plain-text email, exception swallowed |
| `src/main/java/.../event/OrderCreatedEventListener.java` | Reloads order, dispatches emails |
| `src/main/java/.../repository/OrderRepository.java` | Adds findByIdWithItemsAndUser |
