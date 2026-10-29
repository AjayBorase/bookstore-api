# Design explanation & interview prep

## 1. Architecture (layers)
```
HTTP -> Controller (parse/validate request, pick status code)
     -> Service    (business rules + @Transactional)
     -> Repository (Spring Data JPA)  -> Database (H2 / PostgreSQL)
```
- **Controller**: no business logic. `@Valid` checks the JSON, then calls a service.
- **Service**: all rules live here (stock checks, ownership, duplicates). Transactions start here.
- **Repository**: interfaces only; Spring Data generates the SQL.
- **DTOs (Java records)**: the API never exposes entities (no password hashes leaking, no lazy-loading
  surprises, API shape independent of table shape).

## 2. Data model
- `Customer 1 --- * CustomerOrder` (`@OneToMany(mappedBy="customer")` / `@ManyToOne`)
- `CustomerOrder 1 --- * OrderItem` (`cascade = ALL, orphanRemoval = true`: items live and die with the order)
- `OrderItem * --- 1 Book`
- `OrderItem.unitPrice` is a **snapshot** of the price at purchase time; changing a book's price later
  must not change old orders.
- The order entity is named `CustomerOrder` (table `orders`) because `ORDER` is a reserved SQL/JPQL word.
- All associations are `LAZY`; DTO mapping happens inside the service transaction
  (`spring.jpa.open-in-view=false`), and `default_batch_fetch_size` avoids the N+1 query problem on lists.

## 3. The most important method: `OrderService.placeOrder`
1. Customer must exist, else **404**.
2. Duplicate lines are merged (book 1 x2 + book 1 x3 -> x5) so the stock check is correct.
3. Books are locked in ascending id order with `SELECT ... FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`).
   - Locking stops two simultaneous orders from reading the same stock and overselling.
   - Same lock order in every transaction prevents deadlocks.
4. If `stock < quantity` -> `InsufficientStockException` (**409**).
5. Stock is decremented, an `OrderItem` is added with the current price, total is computed with `BigDecimal`.
6. Any `RuntimeException` rolls back the **entire** transaction, so a failure on item 2 undoes item 1
   (proved by `placeOrder_secondItemOutOfStock_rollsBackFirstItem`).

`cancelOrder` locks the order row, rejects double cancellation (409) and adds the stock back.

## 4. Error handling
`GlobalExceptionHandler` (`@RestControllerAdvice`) maps exceptions to status codes in one format:
`ResourceNotFound` 404, `Conflict/Duplicate/InsufficientStock` 409, validation + malformed JSON + bad sort field 400,
`Forbidden` 403, bad credentials 401. It extends `ResponseEntityExceptionHandler` so Spring's own errors
(wrong HTTP method, unknown URL...) use the same JSON. 401/403 raised by the security filter chain are written by
`RestSecurityErrorHandler` because `@ControllerAdvice` cannot see exceptions thrown before the controller.

## 5. Security (bonus)
- Passwords are stored as **BCrypt** hashes.
- `POST /api/auth/login` verifies credentials via `AuthenticationManager` and returns a signed **JWT (HS256)**
  whose subject is the email and which carries a `role` claim; expiry is 24 h.
- `JwtAuthFilter` runs on each request, validates the token and loads the user, so the API is **stateless**
  (no session, hence CSRF is disabled).
- Rules in `SecurityConfig`: books readable by everyone, changed by ADMIN only; customers API ADMIN only;
  orders need login. Ownership ("a customer sees only their own orders") is enforced in `OrderService` through
  `requesterId` (null for admins).
- Public registration can only create CUSTOMERs, never ADMINs.

## 6. Pagination & filtering (bonus)
JPA `Specification`s build the WHERE clause only from the filters that were provided
(`BookSpecifications`, `OrderSpecifications`). `Pageable` comes from `?page=&size=&sort=`; max page size is 100.
Results are wrapped in `PageResponse` for a stable JSON shape.

## 7. Likely interview questions
- **Why DTOs instead of returning entities?** Security, decoupling, avoids lazy-loading/serialisation loops.
- **What does `@Transactional` do here?** Wraps the method in one DB transaction; runtime exceptions roll back.
- **How do you prevent overselling?** Pessimistic row locks (`FOR UPDATE`) + check inside the same transaction.
  Alternative: optimistic locking with `@Version` and retry.
- **Why LAZY?** Don't load data you don't need; mapping is done inside the transaction.
- **`mappedBy` meaning?** The owning side is `CustomerOrder.customer` (it holds the FK); `Customer.orders` is the inverse side.
- **Why BigDecimal for money?** `double` has rounding errors.
- **Why 409 for insufficient stock and not 400?** The request is valid, but conflicts with the current state of the resource.
- **How does JWT auth work end to end?** Login -> token -> client sends `Authorization: Bearer` -> filter validates
  signature/expiry -> user placed in `SecurityContext` -> URL rules + ownership checks.
- **How would you scale it?** Move from `create-drop`/`update` to Flyway migrations, add refresh tokens,
  caching for the book catalogue, and index the filter columns.

## 8. Known limitations (be honest in the interview)
- Schema is created by Hibernate (`ddl-auto`); `database/schema.sql` is the equivalent hand-written PostgreSQL schema.
  A production system would use Flyway/Liquibase.
- No refresh tokens / logout / password reset.
- Order list does not filter by date range.
