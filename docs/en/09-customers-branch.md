# Branch: feature/customers

This branch adds the Customer resource. It is simpler than products (no caching, no
optimistic locking), but it introduces an important pattern: enforcing uniqueness at the
service layer, case-insensitively, and protecting referential integrity before delete.

---

## What you will learn

- How to enforce case-insensitive email uniqueness at the service and repository layers
- How to implement partial-name search with JPQL `LIKE` and `LOWER()`
- How to protect referential integrity by checking related records before delete
- How to surface a domain rule violation as a `BusinessRuleException` (422) rather than
  letting a database constraint violation bubble up as a 500

---

## What it delivers

### Customer entity and repository

`Customer` has `name`, `email`, and `phone` fields. `CustomerRepository` provides:

- `existsByEmailIgnoreCase(String email)` - used by create and update to enforce uniqueness
- `findByNameContainingIgnoreCase(String name, Pageable pageable)` - for partial-name search
- `findByEmailIgnoreCase(String email)` - for email lookup

### CustomerServiceImpl

`create()` and `update()` both call `existsByEmailIgnoreCase` before saving. If the email is
already taken (by a different customer on update), a `BusinessRuleException` is thrown with
a human-readable message. The 422 status code signals to the client that the request was
well-formed but cannot be processed due to a domain constraint.

> **The original bug:** The first version of `create()` used `existsByEmail` (case-sensitive),
> while `update()` used `equalsIgnoreCase`. This meant two customers could share the same
> email address if the case differed - `ada@example.com` and `ADA@EXAMPLE.COM` were treated
> as different. The fix renamed the repository method to `existsByEmailIgnoreCase` and used
> it consistently in both create and update.

### Delete guard

`CustomerServiceImpl.delete()` calls `OrderRepository.existsByCustomerId(id)` before
removing the customer. If any orders reference this customer, a `BusinessRuleException` is
thrown. This prevents a `DataIntegrityViolationException` (the foreign key constraint on
`orders.customer_id`) from reaching the client as a 500 error.

> **Why not rely on the database constraint?** The database constraint is the definitive
> safety net, but a raw constraint violation from Hibernate produces a generic 500 response
> with an internal error message. Checking in the service layer gives control over the error
> message, the HTTP status code, and the response format.

### CustomerController

Standard CRUD endpoints under `/api/v1/customers`. The list endpoint supports an optional
`name` query parameter for partial-name search.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../entity/Customer.java` | Customer JPA entity |
| `src/main/java/.../repository/CustomerRepository.java` | Custom query methods |
| `src/main/java/.../service/CustomerService.java` | Service interface |
| `src/main/java/.../service/impl/CustomerServiceImpl.java` | Delete guard + uniqueness |
| `src/main/java/.../controller/CustomerController.java` | REST endpoints |
| `src/main/resources/db/migration/V1__init.sql` | customers table (initial migration) |
