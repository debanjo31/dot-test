# Money Transfer Service

A production-oriented Spring Boot service that simulates an atomic money transfer between two NGN accounts. It records every valid processing outcome, supports filtered transaction history, calculates commissions in a scheduled job, and materializes daily summaries.

## Highlights

- Java 17 and Spring Boot 3.5
- PostgreSQL with versioned Flyway migrations
- Atomic debit/credit with deterministic pessimistic locking
- Cross-instance idempotency for safe client retries
- Distributed scheduler locks suitable for multiple running service instances
- OpenAPI documentation, RFC-style errors, correlation IDs, health probes, and Prometheus metrics
- Unit and PostgreSQL Testcontainers integration tests, including concurrent transfers
- A focused Docker Compose environment for running the application locally

## API overview

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/v1/transfers` | Process an idempotent account-to-account transfer |
| `GET` | `/api/v1/transactions` | Search and page through transaction history |
| `GET` | `/api/v1/transaction-summaries/{date}` | Retrieve totals for a business date |

## Architecture

The project is organized by business capability. Controllers own HTTP concerns, application services coordinate use cases, pure policies calculate fees and commissions, and repository adapters own persistence.

### Transfer consistency

Each request executes in one database transaction:

1. Atomically claim the `Idempotency-Key` with `INSERT ... ON CONFLICT DO NOTHING`.
2. Return the existing result for a same-key/same-payload replay, or `409` if the payload changed.
3. Lock both account rows in account-number order to avoid lost updates and reduce deadlock risk.
4. Verify accounts, currencies, and available balance.
5. Debit the source, credit the destination, and finalize the transaction status in the same commit.

An unexpected exception rolls the entire operation back. A missing account or insufficient balance is a processed business outcome and is retained in the transaction history.

### Money rules

All amounts use `BigDecimal`, two decimal places, and `HALF_UP` rounding.

```text
transaction fee = min(amount x 0.5%, NGN 100.00)
billed amount   = amount + transaction fee
commission      = transaction fee x 20%
```

For a transfer of NGN 1,000.00, the fee is NGN 5.00, the source is billed NGN 1,005.00, the destination receives NGN 1,000.00, and the later commission is NGN 1.00.

## Run locally

Prerequisites: Docker Engine or Docker Desktop with Docker Compose.

From the repository root, run:

```bash
docker compose up --build
```

The local profile creates two accounts only when they do not already exist:

| Account | Opening balance |
|---|---:|
| `1000000001` | NGN 100,000.00 |
| `1000000002` | NGN 50,000.00 |

Stop the environment with `docker compose down`. Add `--volumes` only when you intentionally want to erase the local database.

### Run with a local JDK

This option requires JDK 17 and PostgreSQL. Copy the relevant values from `.env.example`, start PostgreSQL, and run:

```bash
./mvnw spring-boot:run
```

Flyway applies the schema automatically. The default profile does not create demo accounts; use `SPRING_PROFILES_ACTIVE=local` when demo data is wanted.

## API examples

### Process a transfer

`Idempotency-Key` is required and should identify one logical client request.

```bash
curl --request POST http://localhost:8080/api/v1/transfers \
  --header "Content-Type: application/json" \
  --header "Idempotency-Key: transfer-20260829-001" \
  --data '{
    "sourceAccountNumber": "1000000001",
    "destinationAccountNumber": "1000000002",
    "amount": 1000.00,
    "description": "August rent"
  }'
```

A successful first request returns `201 Created`. Repeating the same successful request with the same key returns `200 OK`, an `Idempotent-Replay: true` header, and the original transaction without another debit. Reusing the key with different content returns `409 Conflict`. A request referencing a missing source or destination account returns `404 Not Found` with an `ACCOUNT_NOT_FOUND` problem response; the failed attempt is retained in transaction history.

Final transaction statuses are:

- `SUCCESSFUL`: balances moved atomically.
- `INSUFFICIENT_FUNDS`: the source cannot cover amount plus fee.
- `FAILED`: a processing prerequisite, such as an account, was missing.

### Search transactions

```bash
curl "http://localhost:8080/api/v1/transactions?status=SUCCESSFUL&accountNumber=1000000001&from=2026-08-01T00:00:00Z&to=2026-09-01T00:00:00Z&page=0&size=20&direction=DESC"
```

All filters are optional. `accountNumber` matches the source or destination, `from` is inclusive, `to` is exclusive, and page size is limited to 100.

### Retrieve a daily summary

```bash
curl http://localhost:8080/api/v1/transaction-summaries/2026-08-29
```

Today is calculated live. Older dates normally return scheduled materializations; if one is absent after downtime, the endpoint safely calculates a read-only fallback. Future dates are rejected.

## Scheduled processing

Schedules use the configured business timezone, defaulting to `Africa/Lagos`.

- `00:05`: process all transactions whose commission has not yet been evaluated. Every successful transfer is commission-worthy; other outcomes receive zero commission.
- `00:15`: regenerate yesterday's summary and backfill any missing historical summaries.

Both jobs are idempotent and protected by PostgreSQL-backed ShedLock using database time. If the service is deployed as several processes or Kubernetes pods, only one instance executes a given job while the others skip it. Processing old unprocessed rows rather than only "yesterday" also recovers automatically after downtime.

## Configuration

| Environment variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/money_transfer` | JDBC connection URL |
| `DB_USERNAME` | `money_transfer` | Database user |
| `DB_PASSWORD` | `money_transfer` | Database password; override outside local use |
| `DB_POOL_SIZE` | `10` | Maximum Hikari pool size per replica |
| `BUSINESS_TIME_ZONE` | `Africa/Lagos` | Summary calendar and scheduler zone |
| `COMMISSION_CRON` | `0 5 0 * * *` | Spring cron for commission processing |
| `SUMMARY_CRON` | `0 15 0 * * *` | Spring cron for summary generation |
| `COMMISSION_BATCH_SIZE` | `500` | Transactions committed per commission batch |


## Design decisions

- Currency is intentionally restricted to NGN; FX conversion is out of scope.
- The source pays amount plus fee and the destination receives only the requested amount.
- Historical account numbers are denormalized into transactions so audit records remain readable if account data changes.
- Authentication and authorization are outside this service's current boundary and can be integrated at the gateway or service layer when an identity contract is available.
- Flyway is the schema source of truth; `ddl-auto=validate` detects drift without mutating production schemas.
- Deployment manifests are intentionally omitted because registry, namespace, ingress, secret management, and resource policies are environment-specific. The application remains ready for multiple replicas through database-backed idempotency, row locking, distributed scheduler locking, health probes, graceful shutdown, and environment-based configuration.
- ShedLock prevents duplicate scheduled work because ordinary Spring scheduling runs independently in every application instance.
