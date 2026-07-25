# E-Commerce Backend API (Microservices)

A microservices-based e-commerce backend built with Spring Boot and Spring Cloud, demonstrating service discovery, inter-service communication, and distributed caching.

> **Status:** Core flow complete (registration/login, product catalog with caching, order placement with live stock/price validation). API Gateway and Spring Security/JWT are intentionally out of scope for this phase — see [Known Limitations](#known-limitations--future-scope) below.

---

## Table of Contents
- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Services](#services)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Running the Project](#running-the-project)
- [API Reference](#api-reference)
- [Testing with Postman](#testing-with-postman)
- [Design Decisions](#design-decisions--interview-notes)
- [Known Limitations & Future Scope](#known-limitations--future-scope)
- [Project Structure](#project-structure)

---

## Architecture Overview

```
                              ┌────────────┐
                              │   Client   │
                              │ (Postman)  │
                              └─────┬──────┘
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
            ┌───────────────┐ ┌──────────────┐ ┌──────────────┐
            │ User Service  │ │Product Service│ │ Order Service│
            │   :8081       │ │    :8082      │ │    :8083     │
            └───────┬───────┘ └───────┬───────┘ └──────┬───────┘
                    │                 │                │
                    │                 │   ◄── OpenFeign call
                    │                 │       (via Eureka)
                    ▼                 ▼                │
            ┌───────────────────────────────┐          │
            │        Eureka Server          │◄─────────┘
            │           :8761               │  (all services register here)
            └────────────────────────────────┘

            Product Service also talks to:
            ┌────────────┐        ┌────────────┐
            │   Redis     │        │  Oracle DB │
            │  (cache)    │        │            │
            └────────────┘        └────────────┘
```

**Request flow for placing an order:**
1. Client registers/logs in via **User Service**
2. Client browses products via **Product Service** (served from Redis cache after the first request)
3. Client places an order via **Order Service**
4. Order Service calls Product Service **live**, over HTTP, resolved through Eureka (not a hardcoded URL) via **OpenFeign**, to verify current stock and price
5. If stock is sufficient, the order is saved with the total calculated from the live price (never from client input)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.2 |
| Service Discovery | Spring Cloud Netflix Eureka |
| Inter-service Communication | OpenFeign |
| Persistence | Spring Data JPA (Hibernate) |
| Database | Oracle Database 21c |
| Caching | Redis (Spring Cache abstraction) |
| Build Tool | Maven |
| Boilerplate Reduction | Lombok |
| API Testing | Postman |

**Deliberately excluded from this phase:** Spring Security / JWT, Spring Cloud Gateway, Bean Validation (`spring-boot-starter-validation`). Password hashing and request validation are handled manually — see [Design Decisions](#design-decisions--interview-notes).

---

## Services

| Service | Port | Responsibility |
|---|---|---|
| **eureka-server** | 8761 | Service registry — every other service registers here and discovers peers by name |
| **user-service** | 8081 | User registration and login, password hashing |
| **product-service** | 8082 | Product catalog CRUD, Redis cache-aside caching |
| **order-service** | 8083 | Order placement, calls Product Service via OpenFeign to validate stock/price |

---

## Prerequisites

Install and have running before you start:

1. **JDK 17** (`java -version` to confirm)
2. **Maven** (bundled with most IDEs, or install standalone)
3. **Oracle Database 21c** — running locally or accessible remotely
4. **Redis** — for Product Service's cache
   - Easiest: `docker run -d -p 6379:6379 redis`
   - Or install natively (Linux/Mac) or via WSL (Windows)
5. **Eclipse / IntelliJ** — any IDE with Maven support
6. **Postman** — for testing the APIs

---

## Database Setup

Unlike MySQL, Oracle does **not** auto-create a database/schema for you — each service needs its own Oracle user/schema created once before first run.

Run this in SQL*Plus (or your Oracle DB tool) for each service, replacing the placeholder password:

```sql
-- User Service
CREATE USER ecommerce_users IDENTIFIED BY your_oracle_password;
GRANT CONNECT, RESOURCE, DBA TO ecommerce_users;

-- Product Service
CREATE USER ecommerce_products IDENTIFIED BY your_oracle_password;
GRANT CONNECT, RESOURCE, DBA TO ecommerce_products;

-- Order Service
CREATE USER ecommerce_orders IDENTIFIED BY your_oracle_password;
GRANT CONNECT, RESOURCE, DBA TO ecommerce_orders;
```

> Each service owns its own schema — a core microservices principle: **no service reads or writes another service's tables directly.** All cross-service data access happens over HTTP (OpenFeign), never a shared database.

After creating each schema, update the matching `application.properties` file with your actual username/password (they currently contain placeholder values).

Hibernate will auto-create the actual **tables** inside each schema on first run (`spring.jpa.hibernate.ddl-auto=update`) — you only need to create the schema/user manually.

---

## Running the Project

**Start order matters.** Each service depends on the one before it being ready.

```
1. Eureka Server     (wait ~10s for it to fully start)
2. Product Service    (wait ~10s — it needs to register with Eureka)
3. User Service
4. Order Service      (needs Product Service registered, since it calls it via Feign)
```

For each service:
1. Import the project into your IDE as a **Maven project**
2. Confirm `application.properties` has correct DB credentials
3. Run the `*Application.java` main class

**Verify everything is connected:**
- Open `http://localhost:8761` — the Eureka dashboard should list `PRODUCT-SERVICE`, `USER-SERVICE`, and `ORDER-SERVICE` under "Instances currently registered with Eureka"
- If a service is missing, check its console logs for connection errors (DB credentials, Redis not running, etc.)

---

## API Reference

### User Service (`:8081`)

| Method | Endpoint | Body | Description |
|---|---|---|---|
| POST | `/api/users/register` | `{ "name", "email", "password" }` | Register a new user (role always defaults to `CUSTOMER`) |
| POST | `/api/users/login` | `{ "email", "password" }` | Login, returns user details (no token — JWT is future scope) |
| GET | `/api/users/{id}` | — | Fetch user by id |

### Product Service (`:8082`)

| Method | Endpoint | Body | Description |
|---|---|---|---|
| POST | `/api/products` | `{ "name", "description", "price", "stock", "category" }` | Create a product |
| GET | `/api/products/{id}` | — | Get one product (cached in Redis) |
| GET | `/api/products` | — | List all products (not cached) |
| PUT | `/api/products/{id}` | same as create | Update a product (refreshes cache) |
| DELETE | `/api/products/{id}` | — | Delete a product (evicts cache) |

### Order Service (`:8083`)

| Method | Endpoint | Body | Description |
|---|---|---|---|
| POST | `/api/orders` | `{ "userId", "items": [{ "productId", "quantity" }] }` | Place an order — validates stock/price live against Product Service |
| GET | `/api/orders/{id}` | — | Get order by id |
| GET | `/api/orders?userId=1` | — | Get all orders for a user |

**Sample error responses:**
```json
// 409 - insufficient stock
{ "timestamp": "...", "status": 409, "message": "Insufficient stock for product 'Wireless Mouse'..." }

// 404 - product referenced in order doesn't exist
{ "timestamp": "...", "status": 404, "message": "Referenced product does not exist" }
```

---

## Testing with Postman

Import [`ecommerce-microservices_postman_collection.json`](./ecommerce-microservices_postman_collection.json) into Postman. It contains pre-built requests with `pm.test()` assertions for all three services, including:
- Duplicate registration / wrong password rejection
- Cache hit vs. cache miss timing comparison for product lookups
- Cache freshness check after an update
- Insufficient stock and nonexistent product error handling

Run requests top-to-bottom within each folder — later requests reuse ids captured automatically from earlier responses (via Postman collection variables), so no manual copy-pasting of ids is needed.

---

## Design Decisions & Interview Notes

A few deliberate choices worth being able to explain out loud:

- **Layered architecture** (Controller → Service → Repository) in every service — keeps HTTP concerns, business logic, and data access independently testable and changeable.
- **DTOs, never Entities, cross the API boundary** — prevents leaking internal fields (like password hashes) and decouples the API contract from the DB schema.
- **Password hashing without Spring Security**: manual SHA-256 + random salt via `java.security` (zero extra dependencies), since the full Security starter was excluded from this phase. Documented in code as a conscious trade-off — BCrypt via Spring Security is the production-grade upgrade path.
- **No shared DTOs between services**: Order Service keeps its own local copy of `ProductDto` rather than importing Product Service's class — avoids tight coupling between independently deployable services.
- **Price is captured at order time**, not looked up live when displaying an order later — an order should reflect what the customer actually agreed to pay, even if the product's price changes afterward.
- **Cache-aside pattern** (not write-through) for Redis in Product Service — reads populate the cache lazily; writes evict/refresh rather than trying to keep cache and DB in perfect lockstep on every write.

---

## Known Limitations & Future Scope

Being upfront about these is a stronger interview answer than pretending they don't exist:

- **Stock is checked but not decremented** — placing an order validates sufficient stock exists but doesn't reduce it in Product Service. Two simultaneous orders for the same last unit could both currently succeed. Planned fix: an atomic conditional-update endpoint (`UPDATE products SET stock = stock - :qty WHERE id = :id AND stock >= :qty`) called from Order Service after validation.
- **No API Gateway** — clients call each service directly on its own port instead of through a single entry point. Spring Cloud Gateway would add centralized routing (and a natural place to add rate limiting).
- **No authentication/authorization** — Spring Security + JWT excluded from this phase; currently `userId` is passed directly in request bodies rather than derived from a validated token, and there's no role-based access control (e.g. only admins should be able to create/delete products).
- **No automated tests** — `spring-boot-starter-test` is included in each service but currently unused; unit tests for the service layer (with Mockito) are a planned addition.
- **No circuit breaker** on the Order → Product Feign call — if Product Service is slow/down, requests currently just time out (per the configured `connect-timeout`/`read-timeout`) rather than failing fast via something like Resilience4j.

---

## Project Structure

```
ecommerce-backend/
├── eureka-server/
│   └── src/main/java/com/ecommerce/eureka/server/
├── user-service/
│   └── src/main/java/com/ecommerce/user/service/
│       ├── entity/ | dto/ | repository/ | service/ | controller/ | exception/
├── product-service/
│   └── src/main/java/com/ecommerce/product/service/
│       ├── entity/ | dto/ | repository/ | service/ | controller/ | exception/ | config/
├── order-service/
│   └── src/main/java/com/ecommerce/order/service/
│       ├── entity/ | dto/ | repository/ | service/ | controller/ | exception/ | client/
└── ecommerce-microservices_postman_collection.json
```

Each service follows the same layered structure:
- `entity/` — JPA entities (DB-mapped classes)
- `dto/` — request/response shapes that actually cross the API
- `repository/` — Spring Data JPA interfaces
- `service/` (+ `service/impl/`) — business logic, interface + implementation
- `controller/` — REST endpoints, no business logic
- `exception/` — custom exceptions + centralized `@RestControllerAdvice` handler
- `config/` (Product Service only) — Redis cache configuration
- `client/` (Order Service only) — OpenFeign client interfaces
