# Bookstore Management REST API

Spring Boot 3 (Java 17) backend for managing **books, customers and orders**.
Layered architecture (Controller -> Service -> Repository), JPA relationships, transactional order
placement with stock consistency, centralized error handling, integration tests, plus the bonus
goals: **pagination/filtering** and **JWT authentication with ADMIN / CUSTOMER roles**.

> Backend only. The assignment asks for a backend, so there is no frontend to run.

## Requirements
- Java 17+ (tested on 17 and 21)
- Maven 3.8+ (or open the project in IntelliJ / VS Code / Eclipse and run `BookstoreApplication`)
- PostgreSQL 14+ (optional; the default profile uses in-memory H2)

## Quick start (zero setup, H2 in-memory)
```bash
mvn spring-boot:run
```
API is at `http://localhost:8080`. Demo data is created on start-up:

| Role     | Email               | Password       |
|----------|---------------------|----------------|
| ADMIN    | admin@bookstore.com | Admin@123      |
| CUSTOMER | john@example.com    | Customer@123   |

## Run with PostgreSQL
1. Create the DB and schema (either way):
   ```bash
   docker compose up -d                      # starts Postgres 16 and loads database/schema.sql
   # or, with your own Postgres:
   createdb bookstore && psql -d bookstore -f database/schema.sql
   ```
2. Start the app with the `postgres` profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=postgres
   ```
   Defaults: `jdbc:postgresql://localhost:5432/bookstore`, user `postgres`, password `postgres`.
   Override with env vars `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. Set `JWT_SECRET` (32+ chars) in real use.

## Run the tests
```bash
mvn test
```
`OrderPlacementIntegrationTest` covers: success, insufficient stock (409), invalid customer (404), unknown book,
validation errors (400), atomic rollback of multi-item orders, duplicate-line merging, cancel + stock restore,
and role-based access. `BookApiIntegrationTest` covers CRUD, duplicates, validation, pagination and filtering.

## Endpoints
| Method | URL | Access | Notes |
|---|---|---|---|
| POST | /api/auth/register | public | creates a CUSTOMER, returns JWT |
| POST | /api/auth/login | public | returns JWT |
| GET | /api/books | public | filters: `title, author, category, minPrice, maxPrice, inStock`; paging: `page, size, sort` |
| GET | /api/books/{id} | public | |
| POST / PUT / DELETE | /api/books[/{id}] | ADMIN | |
| GET/POST/PUT/DELETE | /api/customers[/{id}] | ADMIN | |
| POST | /api/orders | logged in | CUSTOMER can only order for themselves |
| GET | /api/orders | logged in | filters: `customerId, status`; CUSTOMER sees only own orders |
| GET | /api/orders/{id} | owner or ADMIN | |
| POST | /api/orders/{id}/cancel | owner or ADMIN | restores stock |

Send the token as `Authorization: Bearer <token>`.

## Try it with curl
```bash
# login as admin
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@bookstore.com","password":"Admin@123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')

# list books (public), filtered + paged
curl "localhost:8080/api/books?category=programming&inStock=true&page=0&size=5&sort=price,desc"

# add a book (ADMIN)
curl -X POST localhost:8080/api/books -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Refactoring","author":"Martin Fowler","isbn":"9780134757599","price":47.50,"stock":12,"category":"Programming"}'

# place an order (customer id 2 = John in the seed data)
curl -X POST localhost:8080/api/orders -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"customerId":2,"items":[{"bookId":1,"quantity":2},{"bookId":2,"quantity":1}]}'

# cancel it
curl -X POST localhost:8080/api/orders/1/cancel -H "Authorization: Bearer $TOKEN"
```

## Error format (all endpoints)
```json
{ "timestamp": "2026-10-27T10:15:30Z", "status": 409, "error": "Conflict",
  "message": "Insufficient stock for 'Effective Java': requested 11, available 10",
  "path": "/api/orders", "details": [] }
```
400 validation / bad input, 401 not logged in, 403 not allowed, 404 not found, 409 conflict (stock, duplicates).

See `docs/EXPLANATION.md` for the design decisions and interview Q&A.
