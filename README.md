# Cinema Booking System

<!-- Java version -->
<a href="https://img.shields.io/badge/Java-21-blue.svg?logo=Java">
  <img src="https://img.shields.io/badge/Java-21-blue.svg?logo=Java" alt="Java version" />
</a>

<!-- Spring Boot -->
<a href="https://github.com/spring-projects/spring-boot/releases/tag/v4.0.6">
  <img src="https://img.shields.io/badge/SpringBoot-4.0.6-grey.svg?logo=Spring" alt="Spring Boot" />
</a>

<!-- PostgreSQL -->
<a href="https://img.shields.io/badge/PostgreSQL-17-blue.svg?logo=PostgreSQL">
  <img src="https://img.shields.io/badge/PostgreSQL-17-blue.svg?logo=PostgreSQL" alt="PostgreSQL" />
</a>

A Spring Boot backend for booking cinema seats, built around a **from-scratch Strict Two-Phase Locking (Strict 2PL) transaction engine**.

The cinema domain (movies, showings, seats, bookings) is the *vehicle*; the core contribution is the custom concurrency layer that provides ACID guarantees **without** relying on `@Transactional`, `SELECT ... FOR UPDATE`, JPA/Hibernate, or any framework-provided locking. All database access is plain SQL via Spring's `JdbcClient`, and the engine implements transaction management, lock management, deadlock detection (wait-for graph), and compensation-based rollback by hand.

---

## Table of Contents

- [Architecture](#architecture)
- [Concurrency Control](#concurrency-control)
  - [Supported Scenarios](#supported-scenarios)
- [Security](#security)
- [API Reference](#api-reference)
- [Database Model](#database-model)
- [Configuration](#configuration)
- [Local Setup](#local-setup)
- [Project Structure](#project-structure)
- [Dependencies](#dependencies)
- [Code Style](#code-style)

---

## Architecture

The application is a layered Spring Boot service:

```
HTTP (controllers)
      │  REST + JWT auth + @PreAuthorize role checks
      ▼
Services (MovieService, ShowingService, SeatService, BookingService, …)
      │  business logic; write paths wrapped in TransactionManager.execute(...)
      ▼
Custom 2PL Engine  ◀── the academic core
      │  lock → execute → log → commit/abort
      ▼
Repositories (JdbcClient + plain SQL)
      ▼
PostgreSQL (schema owned exclusively by Liquibase)
```

Key design rules enforced throughout the codebase:

- **No framework concurrency primitives.** `@Transactional`, `SELECT FOR UPDATE`, JPA/Hibernate, and optimistic-locking annotations are all forbidden. Concurrency is handled only by the custom engine.
- **`JdbcClient` + plain SQL** for all data access. Repositories use dedicated `RowMapper` classes (no inline lambdas).
- **Liquibase owns the schema.** `spring.sql.init.mode: never`; migrations live in `src/main/resources/liquibase/migrate`.
- **Constructor injection** everywhere via Lombok `@RequiredArgsConstructor`; beans (e.g. `JdbcClient`, security) declared explicitly in the `config` package.
- **Immutable models** as Java records; DTOs are records validated at the controller boundary.

### The 2PL Engine

Every concurrency-sensitive write goes through a single entry point:

```java
transactionManager.execute(ctx -> {
    ctx.lockExclusive(BOOKED_SEAT, encode(showingId, seatId));   // 1. acquire lock first
    if (isSeatAlreadyBooked(ctx, showingId, seatId)) {            // 2. read under lock
        throw new SeatAlreadyBookedException(showingId, seatId);
    }
    long bookingId = ctx.insert(BOOKING, INSERT_BOOKING_SQL, ...); // 3. write + log compensation
    ...
});
```

`TransactionManager.execute(...)` wraps the supplied work:

1. **Begin** — create a `Transaction` (status `ACTIVE`) and persist it to `tx_transaction`.
2. **Run** — invoke the lambda with a thread-bound `TransactionContext`. Each `lock*` call acquires a lock *before* the SQL runs; each write logs a compensation into `tx_operation`.
3. **Commit** — on success: release all locks, mark `COMMITTED`.
4. **Abort/Error** — on exception: `RollbackManager` replays compensations in reverse `sequence_number` order, release all locks, mark `ABORTED` (deadlock / lock failure) or `ERROR` (unexpected), then rethrow.

| Component | Responsibility |
|-----------|----------------|
| `TransactionManager` / `TransactionManagerImpl` | Sole entry point; orchestrates begin → run → commit/abort lifecycle |
| `TransactionContext` / `TransactionContextImpl` | Thread-bound handle exposing `lockShared`, `lockExclusive`, `insert`, `update`, `delete`, `queryOptional`, `queryList` |
| `LockManager` | Authoritative in-memory lock table (`Map<ResourceKey, Lock>`) guarded by one intrinsic monitor; `wait()`/`notifyAll()` for blocked waiters |
| `LockCompatibilityService` | SHARED/SHARED compatible; everything else conflicts. Supports lock **upgrade** (sole SHARED holder → EXCLUSIVE) |
| `WaitForGraph` | Directed "transaction waits for transaction" graph; DFS cycle detection (handles cycles of length > 2) |
| `DeadlockDetector` | Runs cycle detection on each blocked acquire |
| `OperationExecutor` | Executes the SQL and appends the compensation log entry |
| `RollbackManager` | Replays compensation SQL in reverse order on abort |

**Deadlock policy: detector-becomes-victim.** When a transaction's lock request would close a cycle in the wait-for graph, *that* transaction is chosen as the victim and throws `DeadlockDetectedException` — no cross-thread abort signalling is needed.

**Lock acquisition ordering.** The public `BookingService.createBooking(...)` sorts seat IDs ascending so all well-behaved concurrent bookings acquire locks in the same order and cannot spontaneously deadlock. The demo harness deliberately bypasses this via `bookSeatsWithLockOrder(...)` to construct cross-thread orderings that *do* deadlock.

---

## Concurrency Control

The booking flow acquires locks as follows:

- **SHARED** lock on the parent `(MOVIE, movieId)` and `(SHOWING, showingId)` — so an admin deleting a movie/showing waits behind active bookings.
- **EXCLUSIVE** lock on each seat, keyed by a composite `(BOOKED_SEAT, encode(showingId, seatId))` — this is the contention point that serialises concurrent attempts on the same seat.
- Schema-level safety net: a `UNIQUE (showing_id, seat_id)` constraint on `booked_seat` guarantees single-occupancy even if logic were bypassed.

### Supported Scenarios

These are exercised live through the admin demo endpoints (see [API Reference](#api-reference)). Each returns a `DemoReport` summarising commits vs. aborts per transaction.

#### 1. Single-seat conflict
Two users race to book **the same seat** for the same showing.
**Expected:** 1 `COMMITTED`, 1 `ABORTED` (`SeatAlreadyBookedException`). The winner holds the exclusive seat lock; the loser sees the seat already booked once it acquires the lock.

```
User A ──▶ lock(seat) ──▶ insert ──▶ COMMIT
User B ──▶ wait(seat) ──────────────▶ acquire ──▶ "already booked" ──▶ ABORT
```

#### 2. Classic two-way deadlock
User A books seats in order `[1, 2]`; User B books the **same seats in reverse** `[2, 1]`.
**Expected:** 1 `COMMITTED`, 1 `ABORTED` (`DeadlockDetectedException`).

```
A: lock(1) held ─┐ wait(2)
B: lock(2) held ─┘ wait(1)   ← wait-for cycle A→B→A → victim aborts
```

#### 3. Three-way deadlock cycle
Three users form an `A→B→C→A` cycle: A takes `[seat1, seat2]`, B takes `[seat2, seat3]`, C takes `[seat3, seat1]`.
**Expected:** 2 `COMMITTED`, 1 `ABORTED` (`DeadlockDetectedException`). Proves the wait-for-graph DFS detects cycles longer than 2; whichever transaction closes the cycle is the victim.

#### 4. Bulk-booking atomicity (rollback)
One user books N seats where one seat was **pre-booked** by another user. The whole bulk booking must roll back — every already-inserted `booked_seat` row **and** the parent `booking` row.
**Expected:** 1 `ABORTED`, **no orphan rows**. Demonstrates compensation-based reverse-order rollback.

---

## Security

Authentication is **stateless JWT** (HS256, `io.jsonwebtoken` 0.12.6). Authorization is **role-based** with two roles.

### Roles

| Role    | Capabilities |
|---------|--------------|
| `USER`  | Browse movies/showings/seats, create their own bookings, view and cancel their own bookings |
| `ADMIN` | Everything a `USER` can do, plus create/delete movies, create showings, create seats, and run the concurrency demo endpoints |

New self-registrations via `POST /api/auth/register` are always created as `USER`. `ADMIN` accounts are seeded (see [Local Setup](#local-setup)).

### How it works

1. **Register / Login** (`/api/auth/**`, public) → `AuthService` validates credentials (passwords hashed with `BCryptPasswordEncoder`) and `JwtService` issues a signed token containing the username (`subject`), issuer, and expiry.
2. **Authenticated requests** send `Authorization: Bearer <token>`.
3. `JwtAuthenticationFilter` (a `OncePerRequestFilter`) extracts and validates the token, loads the user via `CinemaUserDetailsService` (backed by `JdbcClient`, **not** JPA), and populates the `SecurityContext`. Invalid/expired/missing tokens fall through unauthenticated — the authorization rules then reject the request.
4. **Authorization**:
   - `SecurityConfig` is stateless (`SessionCreationPolicy.STATELESS`), CSRF disabled, `/api/auth/**` public, everything else authenticated.
   - Method-level `@PreAuthorize("hasRole('ADMIN')")` guards admin-only operations (`@EnableMethodSecurity`).

```yaml
# configured under security.jwt
secret: ${JWT_SECRET}              # HMAC signing key (≥ 32 bytes)
expiration-millis: 86400000        # 24h default
issuer: cinema-booking-system
```

---

## API Reference

All endpoints are under `/api`. Unless marked **Public**, a valid `Bearer` token is required. **ADMIN** marks endpoints that additionally require the `ADMIN` role.

### Authentication

| Method | Path                 | Access | Description |
|--------|----------------------|--------|-------------|
| `POST` | `/api/auth/register` | Public | Register a new `USER`; returns a JWT |
| `POST` | `/api/auth/login`    | Public | Authenticate; returns a JWT |

### Movies

| Method   | Path               | Access | Description |
|----------|--------------------|--------|-------------|
| `GET`    | `/api/movies`      | USER   | List all movies |
| `GET`    | `/api/movies/{id}` | USER   | Get one movie |
| `POST`   | `/api/movies`      | ADMIN  | Create a movie |
| `DELETE` | `/api/movies/{id}` | ADMIN  | Delete a movie (cascades to showings) |

### Showings

| Method | Path                              | Access | Description |
|--------|-----------------------------------|--------|-------------|
| `GET`  | `/api/showings`                   | USER   | List showings; optional `?movieId=` filter |
| `GET`  | `/api/showings/{id}`              | USER   | Get one showing |
| `POST` | `/api/showings`                   | ADMIN  | Create a showing |

### Seats

| Method | Path                          | Access | Description |
|--------|-------------------------------|--------|-------------|
| `GET`  | `/api/seats`                  | USER   | List all seats |
| `GET`  | `/api/showings/{id}/seats`    | USER   | Seat availability for a showing |
| `POST` | `/api/seats`                  | ADMIN  | Create a seat |

### Bookings

| Method   | Path                  | Access | Description |
|----------|-----------------------|--------|-------------|
| `POST`   | `/api/bookings`       | USER   | Book one or more seats for a showing (runs through 2PL) |
| `GET`    | `/api/bookings/me`    | USER   | List the caller's bookings |
| `DELETE` | `/api/bookings/{id}`  | USER   | Cancel one of the caller's bookings (compensating rollback of seats + booking) |

`POST /api/bookings` and `GET /api/bookings/me` resolve the user from the JWT principal — a user can only see and cancel their own bookings (`BookingForbiddenException` otherwise).

### Concurrency Demos (ADMIN)

Each runs the named scenario against the live booking service and returns a `DemoReport` (`{ scenario, commits, aborts, results[] }`).

| Method | Path                                  | Scenario |
|--------|---------------------------------------|----------|
| `POST` | `/api/admin/demo/single-seat-conflict`  | [1 — single-seat conflict](#1-single-seat-conflict) |
| `POST` | `/api/admin/demo/classic-deadlock`      | [2 — two-way deadlock](#2-classic-two-way-deadlock) |
| `POST` | `/api/admin/demo/three-way-deadlock`    | [3 — three-way deadlock](#3-three-way-deadlock-cycle) |
| `POST` | `/api/admin/demo/bulk-booking-atomicity`| [4 — bulk-booking atomicity](#4-bulk-booking-atomicity-rollback) |

Example request bodies:

```jsonc
// single-seat-conflict
{ "showingId": 1, "seatId": 1, "userIdA": 2, "userIdB": 3 }

// classic-deadlock
{ "showingId": 1, "seatA": 1, "seatB": 2, "userIdA": 2, "userIdB": 3 }

// three-way-deadlock
{ "showingId": 1, "seat1": 1, "seat2": 2, "seat3": 3, "userIdA": 2, "userIdB": 3, "userIdC": 4 }

// bulk-booking-atomicity
{ "showingId": 1, "seatIds": [5, 6, 7], "userId": 2, "preBookedSeatId": 6 }
```

---

## Database Model

PostgreSQL 17, schema managed exclusively by Liquibase (`src/main/resources/liquibase/migrate`). There are two table groups: the **business domain** and the **2PL engine state**.

```dbml
// ─── Business domain ───
Table movie {
  id           bigint       [pk, increment]
  title        varchar(50)
  description  varchar(1000)
  genre        varchar(30)
  director     varchar(30)
  language     varchar(30)
  duration     integer
  release_date date
  rating       varchar(50)
  trailer_url  varchar(200)
}

Table showing {
  id             bigint [pk, increment]
  movie_id       bigint [not null, ref: > movie.id, note: 'FK ON DELETE CASCADE']
  screening_date date
}

Table seat {
  id            bigint     [pk, increment]
  row_number    varchar(5)
  column_number varchar(5)
  seat_type     varchar(30) [note: 'REGULAR | VIP']
}

Table cinema_user {
  id           bigint       [pk, increment]
  user_name    varchar(50)  [not null, unique, note: 'uk_cinema_user_user_name']
  password     varchar(100) [not null, note: 'BCrypt hash']
  role         varchar(10)  [not null, note: 'USER | ADMIN']
  email        varchar(50)
  phone_number varchar(10)
}

Table booking {
  id           bigint      [pk, increment]
  user_id      bigint      [not null, ref: > cinema_user.id, note: 'FK ON DELETE CASCADE']
  showing_id   bigint      [not null, ref: > showing.id, note: 'FK ON DELETE CASCADE']
  booking_time timestamp
  status       varchar(25) [note: 'CONFIRMED | CANCELLED']
}

Table booked_seat {
  id         bigint [pk, increment]
  booking_id bigint [not null, ref: > booking.id, note: 'FK ON DELETE CASCADE']
  showing_id bigint [not null, ref: > showing.id, note: 'FK ON DELETE CASCADE']
  seat_id    bigint [not null, ref: > seat.id,    note: 'FK ON DELETE CASCADE']

  indexes {
    (showing_id, seat_id) [unique, note: 'uk_booked_seat_showing_seat — one seat per showing']
  }
}

// ─── 2PL engine state ───
Table tx_transaction {
  id         varchar(36)  [pk, note: 'UUID']
  status     varchar(16)  [not null, note: 'ACTIVE | COMMITTED | ABORTED | ERROR']
  started_at timestamp    [not null]
  ended_at   timestamp
}

Table tx_lock {
  id             varchar(36) [pk]
  transaction_id varchar(36) [not null, ref: > tx_transaction.id, note: 'FK ON DELETE CASCADE']
  table_name     varchar(32) [not null]
  resource_id    bigint      [not null]
  lock_type      varchar(16) [not null, note: 'SHARED | EXCLUSIVE']
  acquired_at    timestamp   [not null]

  indexes {
    (table_name, resource_id)
    (transaction_id)
  }
}

Table tx_operation {
  id                       bigint      [pk, increment]
  transaction_id           varchar(36) [not null, ref: > tx_transaction.id, note: 'FK ON DELETE CASCADE']
  sequence_number          integer     [not null]
  operation_type           varchar(16) [not null, note: 'INSERT | UPDATE | DELETE']
  table_name               varchar(32) [not null]
  resource_id              bigint
  compensation_sql         text
  compensation_params_json jsonb
  executed_at              timestamp   [not null]

  indexes {
    (transaction_id, sequence_number) [unique, note: 'uk_tx_operation_tx_seq']
    (transaction_id)
  }
}
```

### Relationships at a glance

- A **movie** has many **showings** (delete cascades).
- A **showing** belongs to a movie and has many **bookings** and **booked_seats**.
- A **cinema_user** has many **bookings**.
- A **booking** belongs to a user + showing and has many **booked_seats**.
- A **booked_seat** ties a booking, showing and seat together; `UNIQUE (showing_id, seat_id)` enforces that a seat can be booked only once per showing.
- The **`tx_*` tables** are the durable record of the 2PL engine: every transaction, the locks it holds, and an append-only operation log used to replay compensations on rollback. `tx_operation.compensation_params_json` is `jsonb` (parsed via the static `JsonParser` utility).

---

## Configuration

Configuration uses Spring profiles. Secrets are injected as environment variables — never committed.

### Profiles

| Profile     | Usage |
|-------------|-------|
| _(default)_ | Production base; all values env-driven; Liquibase context `!local` (excludes demo seed data) |
| `local`     | Local development; Docker Compose datasource; Liquibase context `local` (loads demo seed data); DEBUG logging |

Active profile defaults to `local` (`SPRING_PROFILES_ACTIVE`).

### Environment Variables (default profile)

| Variable                 | Description |
|--------------------------|-------------|
| `DB_URL`                 | JDBC URL for PostgreSQL |
| `DB_USERNAME`            | Database user |
| `DB_PASSWORD`            | Database password |
| `JWT_SECRET`             | HMAC signing key for JWTs (≥ 32 bytes) |
| `JWT_EXPIRATION_MS`      | Token lifetime in ms (default `86400000` = 24h) |
| `SERVER_PORT`            | HTTP port (default `8080`) |
| `SPRING_PROFILES_ACTIVE` | Active profile (default `local`) |

### Liquibase contexts

- Default profile runs migrations with context `!local` — the demo seed data (changeset 10, tagged `context: local`) is **excluded** so it can never reach production.
- `local` profile runs with context `local` — seed data **is** loaded.

`spring.sql.init.mode` is `never`; Liquibase is the only thing that touches the schema.

---

## Local Setup

**Prerequisites:** Java 21, Docker (for PostgreSQL).

1. **Start PostgreSQL** (PostgreSQL 17.2 on `:5432`, db/user/pass `cinema_booking`/`cinema`/`cinema`):

   ```bash
   docker compose -f local-scripts/docker-compose.yaml up -d
   ```

2. **Run the application** (defaults to the `local` profile, which loads demo seed data):

   ```bash
   ./gradlew bootRun
   ```

   Liquibase migrates the schema and seeds demo data on startup.

3. **Seeded demo data** (local profile only). All seeded users share the password `password123`.

   | Username | Role  |
   |----------|-------|
   | `admin`  | ADMIN |
   | `alice`  | USER  |
   | `bob`    | USER  |
   | `carol`  | USER  |

   Plus: 1 movie (`Demo Movie`), 1 showing, and 12 seats (rows A–C × columns 1–4; row C is `VIP`).

4. **Try it out:**

   ```bash
   # Log in as admin
   curl -s -X POST localhost:8080/api/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"userName":"admin","password":"password123"}'

   # Use the returned token
   curl -s localhost:8080/api/movies -H "Authorization: Bearer <token>"

   # Run a concurrency demo (admin)
   curl -s -X POST localhost:8080/api/admin/demo/single-seat-conflict \
     -H "Authorization: Bearer <token>" -H 'Content-Type: application/json' \
     -d '{"showingId":1,"seatId":1,"userIdA":2,"userIdB":3}'
   ```

---

## Project Structure

```
src/main/java/org/example/cinemabookingsystem
├── auth/          # JWT auth: AuthController/Service, JwtService, JwtAuthenticationFilter, DTOs
├── user/          # CinemaUser, Role, repository, CinemaUserDetailsService
├── config/        # SecurityConfig, DataSourceConfig (explicit beans)
├── movie/         # Movie domain: controller, service, repository, DTOs, RowMapper
├── showing/       # Showing domain
├── seat/          # Seat domain
├── booking/       # Booking + BookedSeat domain; SeatLockKey composite key encoding
├── concurrency/   # ◀── the custom 2PL engine
│   ├── api/         # TransactionManager, TransactionContext, exceptions
│   ├── model/       # Transaction, Lock, Operation records + enums + RowMappers
│   ├── repository/  # JdbcClient-backed repositories for tx_* tables
│   └── engine/      # LockManager, WaitForGraph, DeadlockDetector, RollbackManager,
│                    #   OperationExecutor, TransactionManagerImpl, TransactionContextImpl
├── demo/          # ConcurrencyDemoController/Service + scenario DTOs
├── common/        # DomainExceptionHandler (REST error mapping)
└── utils/         # JsonParser (jsonb ↔ JsonNode), JsonParsingException

src/main/resources/liquibase/migrate   # 001–010 changesets (domain + tx_* + seed)
local-scripts/docker-compose.yaml       # local PostgreSQL
```

---

## Dependencies

| Category               | Dependency                                | Version |
|------------------------|-------------------------------------------|---------|
| Language               | Java                                      | 21      |
| Framework              | Spring Boot                               | 4.0.6   |
| Web                    | `spring-boot-starter-webmvc`              | (managed) |
| Data access            | `spring-boot-starter-jdbc` (`JdbcClient`) | (managed) |
| Security               | `spring-boot-starter-security`            | (managed) |
| Validation             | `spring-boot-starter-validation`          | (managed) |
| Migrations             | `spring-boot-starter-liquibase`           | (managed) |
| JWT                    | `io.jsonwebtoken` (jjwt api/impl/jackson) | 0.12.6  |
| JSON                   | `jackson-databind` + `jackson-datatype-jsr310` | (managed) |
| Database driver        | PostgreSQL                                | (runtime) |
| Boilerplate reduction  | Lombok                                    | (managed) |

> Spring Boot 4 has no `spring-boot-starter-json` artifact — Jackson is added explicitly.

### Testing

| Dependency                              | Purpose |
|-----------------------------------------|---------|
| JUnit 5 (`junit-platform-launcher`)     | Test runner |
| `spring-boot-starter-jdbc-test`         | Slice tests for JDBC repositories |
| `spring-boot-starter-security-test`     | Security test support |
| `spring-boot-starter-webmvc-test`       | MVC controller tests |
| Mockito (Spring Boot managed)           | Unit-test mocking |

---

## Code Style

- **Constructor injection** via Lombok `@RequiredArgsConstructor` — no field injection.
- **Immutable models and DTOs** as Java records; validation annotations on DTOs, checked at the controller boundary.
- **Explicit beans** (`JdbcClient`, security components) declared in the `config` package.
- **Dedicated `RowMapper` classes** per entity — no inline lambda mappers.
- **Every JSON column is `jsonb`**; mapped to a `JsonNode` in Java via the static `JsonParser` utility.
- **No framework concurrency** — see [Architecture](#architecture). All transactional behaviour routes through the custom 2PL engine.
- **Single responsibility per class**: controllers handle HTTP only, services hold business logic, repositories hold SQL only.
