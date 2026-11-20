# Branch: feature/shopping-cart

This branch introduces a shopping cart tied to the authenticated user. It demonstrates lazy
entity creation, ownership enforcement, and a session-scoped many-to-many pattern between
users and products without a dedicated join entity.

---

## What you will learn

- How to create a resource lazily on first use rather than on explicit creation
- How to enforce ownership in a service layer (`@AuthenticationPrincipal` passed down to the service)
- How to load a bidirectional association eagerly with `@EntityGraph` on a repository query
- How `@WithMockUser` differs from `.with(user(...))` in `@WebMvcTest` slices with disabled filters
- Why `CartMapper.toResponse` must be a `default` method when the target is a `record`

---

## What it delivers

### Flyway migration

| Migration | What it does |
|---|---|
| V17 | Creates `carts` (one row per user) and `cart_items` (product, quantity, one row per product per cart) tables |

V17 is used instead of V16 because V16 is reserved by `feature/product-images` on a parallel
branch. Choosing V17 upfront avoids a Flyway checksum conflict at merge time.

### Cart / CartItem entities

`Cart` has `@OneToOne AppUser user` and `@OneToMany(cascade = ALL, orphanRemoval = true) List<CartItem> items`.
`CartItem` has `@ManyToOne Cart cart` and `@ManyToOne Product product`, `int quantity`, and a
`@Transient BigDecimal subtotal` computed at read time.

### CartService / CartServiceImpl

`CartServiceImpl` implements the following rules:

- **Lazy creation:** a cart row is created only on the first `addItem` call. `getCart` never
  creates a cart; it returns an empty response if none exists. This avoids orphan rows for
  users who browse without adding anything.
- **Active product check:** products with `active = false` cannot be added.
- **Quantity increment on duplicate:** adding an item already in the cart increments its
  quantity instead of inserting a duplicate row.
- **Ownership check:** `updateItem` and `removeItem` verify that the item belongs to the
  authenticated user's cart before modifying it.

### CartController

Five endpoints, all secured with `hasRole("USER")`:

| Method | URL | Description |
|---|---|---|
| GET | `/api/v1/cart` | View current cart |
| POST | `/api/v1/cart/items` | Add a product to the cart |
| PATCH | `/api/v1/cart/items/{itemId}` | Update item quantity |
| DELETE | `/api/v1/cart/items/{itemId}` | Remove a single item |
| DELETE | `/api/v1/cart` | Clear the entire cart |

The authenticated user is resolved with `@AuthenticationPrincipal` and passed to the service,
never read from the request body.

---

## Key decisions

**`@EntityGraph` on `CartRepository.findByUserId`**
`Cart.items` and `CartItem.product` are lazy by default. Loading them inside a service method
after the initial `save` would trigger additional selects outside the original transaction.
`findByUserId` with `@EntityGraph(attributePaths = {"items", "items.product"})` loads the
full graph in a single join query, and is called after every mutation to return the persisted
state.

**`CartMapper.toResponse` as a `default` method**
MapStruct `@AfterMapping` cannot modify an already-constructed `record`. `toResponse` is a
`default` method that iterates items, computes subtotals, sums the cart total, and builds the
record manually. Same pattern as `ProductMapper.toDetailResponse`.

**`@WithMockUser` instead of `.with(user(...))` in `CartControllerTest`**
With `@AutoConfigureMockMvc(addFilters = false)`, `SecurityContextHolderFilter` never runs,
so `.with(user(...))` does not populate `SecurityContextHolder` and `@AuthenticationPrincipal`
returns `null`. `@WithMockUser` is processed by `WithSecurityContextTestExecutionListener`,
which populates the context before the test method runs, bypassing the filter entirely.

---

## Key files

| File | Purpose |
|---|---|
| `src/main/java/.../entity/Cart.java` | JPA entity, cascade + orphanRemoval |
| `src/main/java/.../entity/CartItem.java` | JPA entity with transient subtotal |
| `src/main/java/.../dto/cart/CartItemRequest.java` | Add-item request DTO |
| `src/main/java/.../dto/cart/CartItemUpdateRequest.java` | Update-quantity DTO |
| `src/main/java/.../dto/cart/CartItemResponse.java` | Per-item response DTO |
| `src/main/java/.../dto/cart/CartResponse.java` | Full cart response DTO |
| `src/main/java/.../mapper/CartMapper.java` | default toResponse with subtotal computation |
| `src/main/java/.../repository/CartRepository.java` | EntityGraph eager-load |
| `src/main/java/.../repository/CartItemRepository.java` | Per-item queries |
| `src/main/java/.../service/CartService.java` | Cart service contract |
| `src/main/java/.../service/impl/CartServiceImpl.java` | Lazy creation, ownership, increment |
| `src/main/java/.../controller/CartController.java` | 5 endpoints, @AuthenticationPrincipal |
| `src/main/resources/db/migration/V17__add_cart_tables.sql` | Schema migration |
