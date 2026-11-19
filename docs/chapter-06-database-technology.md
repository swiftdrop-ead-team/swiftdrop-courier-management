# Chapter 06 – Database Technology

---

## 6.1 Database Technology Overview

SwiftDrop employs a **Database-per-Service** architectural pattern, where each of the eight backend microservices owns and manages its own dedicated logical database. This design is a cornerstone of the microservices philosophy: it ensures strict data isolation, eliminates shared-database coupling between services, and allows each service to evolve its schema independently without risking downstream side effects.

All eight databases are hosted on a single **PostgreSQL 16** instance running inside a Docker container. While the databases share the same physical server, they are logically separated — each microservice connects exclusively to its own database and has no knowledge of the schemas owned by other services. This approach balances operational simplicity (a single database server to manage) with the data sovereignty guarantees of the microservices pattern.

The database layer is built on three foundational pillars:

1. **Relational Data Model**: PostgreSQL provides ACID-compliant transactions, strong referential integrity through foreign key constraints, and advanced indexing capabilities — all essential for a logistics platform where data consistency is non-negotiable.

2. **ORM Abstraction (Hibernate / Spring Data JPA)**: Each microservice uses Spring Data JPA with Hibernate as its Object-Relational Mapping layer. This allows developers to interact with the database using Java entities and repository interfaces rather than writing raw SQL, while still retaining the ability to execute native queries when needed. Hibernate is configured with `ddl-auto: validate`, meaning it verifies that the database schema matches the entity mappings at startup but never auto-generates schema changes — all schema evolution is managed explicitly through migration scripts.

3. **Versioned Schema Migrations (Flyway)**: Database schema changes are managed through Flyway, a lightweight migration tool that tracks every schema version in a `flyway_schema_history` table. Each migration is a numbered SQL file (e.g., `V1__init_parcel_schema.sql`, `V2__dispatch_sync_timestamp.sql`) that is applied in strict sequential order. This ensures that every environment — development, testing, and production — reaches the exact same schema state reproducibly.

### Database Allocation

The following table summarizes the database-to-service mapping:

| Microservice | Database Name | Primary Domain |
|---|---|---|
| Identity Service | `swiftdrop_identity` | Users, roles, OAuth identities, sessions, merchant approvals |
| Parcel Service | `swiftdrop_parcel` | Parcels, status history, delivery estimates, merchant order references |
| Merchant Service | `swiftdrop_merchant` | Merchant profiles, integration contracts, API credentials |
| Dispatch Service | `swiftdrop_dispatch` | Couriers, dispatch assignments, proof of delivery |
| Pricing Service | `swiftdrop_pricing` | Delivery zones, pricing rules, tier discounts, promo codes |
| Tracking Service | `swiftdrop_tracking` | Real-time location updates, movement audit trail |
| Notification Service | `swiftdrop_notification` | Notification templates, delivery records, read state, email preferences |
| AI Assistant Service | `swiftdrop_reports` | Analytics reports, AI-generated insights |

### Why PostgreSQL?

PostgreSQL was selected as the primary database engine for several reasons:

- **Reliability & ACID Compliance**: As a logistics platform, SwiftDrop requires guaranteed data integrity. Every parcel booking, payment calculation, and dispatch assignment must be transactionally consistent.
- **Advanced Data Types**: PostgreSQL supports UUIDs (used extensively as primary keys), JSONB for flexible metadata storage, `NUMERIC` types for precise financial calculations, and `TIMESTAMP WITH TIME ZONE` for accurate temporal tracking across the Asia/Colombo timezone.
- **Indexing & Performance**: The schema makes extensive use of B-tree indexes on frequently queried columns (e.g., `tracking_number`, `status`, `courier_id`, `zone_code`) to ensure sub-millisecond lookups even as data volume grows.
- **Mature JDBC Ecosystem**: The `org.postgresql.Driver` JDBC driver is production-tested and fully compatible with Hibernate's `PostgreSQLDialect`, enabling seamless query generation and result mapping.
- **Container-Friendly**: The `postgres:16-alpine` Docker image is lightweight (~40 MB), starts quickly, and integrates cleanly with Docker Compose health checks (`pg_isready`), making local development and CI/CD pipelines straightforward.

---

## 6.2 Database Technologies Used

This section details every database-related technology, library, and tool used in the SwiftDrop project.

### 6.2.1 Relational Database Management System

| Technology | Version | Role |
|---|---|---|
| **PostgreSQL** | 16 (Alpine) | Primary RDBMS for all microservice databases |
| **Docker** | Latest | Containerization of the PostgreSQL server |

PostgreSQL 16 is deployed as a single Docker service (`swiftdrop-postgres`) with persistent storage via a Docker named volume (`postgres_data`). The server is initialized through an entrypoint script (`infrastructure/database/init.sql`) that creates all eight logical databases and grants privileges to the application user.

### 6.2.2 Object-Relational Mapping (ORM)

| Technology | Version | Role |
|---|---|---|
| **Spring Data JPA** | (Spring Boot 3.4.x) | Repository abstraction layer for type-safe data access |
| **Hibernate** | 6.x (bundled with Spring Boot 3.4.x) | ORM engine for entity-to-table mapping and query generation |
| **Jakarta Persistence API (JPA)** | 3.1 | Standard Java persistence annotations (`@Entity`, `@Table`, `@Column`, etc.) |

Each microservice defines Java entity classes annotated with JPA annotations that map directly to database tables. Spring Data JPA repository interfaces (extending `JpaRepository`) provide CRUD operations, pagination, and derived query methods without requiring implementation code. For example:

```java
@Entity
@Table(name = "parcels")
public class Parcel {
    @Id
    private UUID id;
    @Column(unique = true, nullable = false, length = 30)
    private String trackingNumber;
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;
    // ...
}

public interface ParcelRepository extends JpaRepository<Parcel, UUID> {
    Optional<Parcel> findByTrackingNumber(String trackingNumber);
    List<Parcel> findByStatus(ParcelStatus status);
}
```

Hibernate is configured with the following key settings in every service's `application.yml`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate    # Schema validation only — no auto DDL
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
```

The `ddl-auto: validate` setting is critical: it ensures that the database schema always matches the entity mappings at startup, catching any drift between code and database early, while preventing Hibernate from making unauthorized schema modifications.

### 6.2.3 Database Migration Tool

| Technology | Version | Role |
|---|---|---|
| **Flyway** | (bundled with Spring Boot 3.4.x) | Version-controlled schema migration management |

Flyway is enabled in every microservice and scans the `classpath:db/migration` directory for SQL migration scripts. Each migration file follows the naming convention `V<version>__<description>.sql`, ensuring strict sequential execution. Flyway records every applied migration in the `flyway_schema_history` table, enabling idempotent deployments — running migrations on an already-up-to-date database is a safe no-op.

Configuration in `application.yml`:

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

The migration landscape across all services includes **27 migration files**:

| Service | Migration Count | Key Migrations |
|---|---|---|
| Identity Service | 3 | Auth schema, seed data, account settings |
| Parcel Service | 5 | Parcel schema, dispatch sync, persistent estimates, merchant order reference, pickup date & COD |
| Merchant Service | 2 | Merchant schema, integration contract |
| Dispatch Service | 6 | Dispatch schema, courier seed data, notification recipient, courier email, concurrent assignments, cleanup |
| Pricing Service | 3 | Pricing schema, price quotes & audit fields, delivery rate seed data |
| Tracking Service | 1 | Tracking schema |
| Notification Service | 5 | Notification schema, seed data, read state, email preferences, cleanup |
| AI Assistant Service | 1 | Reports schema |

### 6.2.4 JDBC Driver

| Technology | Version | Role |
|---|---|---|
| **PostgreSQL JDBC Driver** | (bundled with Spring Boot 3.4.x) | Java-to-PostgreSQL communication layer |

The driver class `org.postgresql.Driver` is configured in every service's datasource configuration. Connection URLs follow the pattern:

```
jdbc:postgresql://postgres:5432/<database_name>
```

### 6.2.5 Connection Pooling

| Technology | Version | Role |
|---|---|---|
| **HikariCP** | (default in Spring Boot 3.4.x) | High-performance JDBC connection pool |

Spring Boot automatically configures HikariCP as the default connection pool. HikariCP is chosen for its low-latency connection acquisition and thread-safe pool management, which is essential for handling concurrent requests across multiple microservices sharing the same PostgreSQL instance.

### 6.2.6 Embedded Testing Database

| Technology | Version | Role |
|---|---|---|
| **H2 Database** | (bundled) | In-memory database for unit and integration tests |

During testing (`application-test.yml` profiles), services may use H2 as an in-memory database to enable fast, isolated test execution without requiring a running PostgreSQL instance. This allows the test suite to run independently in CI/CD pipelines.

### 6.2.7 Message Broker (Supplementary Data Store)

| Technology | Version | Role |
|---|---|---|
| **RabbitMQ** | 3 (Alpine) | Asynchronous event bus for inter-service communication |

While not a traditional database, RabbitMQ serves as a durable message store for event-driven communication between microservices. Messages such as `parcel.created`, `parcel.status_updated`, `courier.assigned`, and `notification.dispatch` are published to RabbitMQ exchanges and consumed by downstream services, enabling loose coupling and eventual consistency across the system.

---

## 6.3 Database Management and Data Storage

### 6.3.1 Database Initialization

When the PostgreSQL container starts for the first time, Docker executes the `infrastructure/database/init.sql` script as an entrypoint initialization file. This script:

1. Creates eight logical databases (`swiftdrop_identity`, `swiftdrop_parcel`, `swiftdrop_merchant`, `swiftdrop_dispatch`, `swiftdrop_pricing`, `swiftdrop_tracking`, `swiftdrop_notification`, `swiftdrop_reports`).
2. Uses the `pg_database` system catalog with `WHERE NOT EXISTS` guards to ensure idempotent execution — re-running the script on an existing database does not cause errors.
3. Grants `ALL PRIVILEGES` on each database to the configured application user.

After the databases are created, each microservice's startup sequence triggers Flyway to apply its migration scripts in version order, building the complete schema for that service's bounded context.

### 6.3.2 Schema Design Patterns

The SwiftDrop database schemas follow several consistent design patterns:

**UUID Primary Keys**: Most entities use `UUID` as the primary key type. UUIDs are globally unique, non-sequential identifiers that prevent ID enumeration attacks and facilitate data merging in distributed systems. For example, the `parcels` table uses `id UUID PRIMARY KEY` and the `users` table uses `id UUID PRIMARY KEY`.

**Auto-Incrementing Integer Keys**: Some services (Dispatch, Pricing) use `BIGSERIAL` for auto-incrementing primary keys where internal, system-generated identifiers are sufficient and human-readable sequential IDs are preferred.

**Timestamps with Timezone**: All temporal columns use `TIMESTAMP WITH TIME ZONE` to store dates in UTC, ensuring correct behavior regardless of the server or client timezone. The application timezone is configured as `Asia/Colombo` via the `TZ` environment variable and `user.timezone` JVM property.

**Referential Integrity**: Foreign key constraints with `ON DELETE CASCADE` or `ON DELETE RESTRICT` are used to enforce data integrity at the database level. For example, `parcel_status_history` references `parcels(id) ON DELETE CASCADE`, ensuring that deleting a parcel automatically removes its status history.

**Strategic Indexing**: Every table includes indexes on columns that are frequently used in `WHERE`, `JOIN`, and `ORDER BY` clauses. For example, the `parcels` table has indexes on `tracking_number`, `sender_id`, `status`, `assigned_courier_id`, and `created_at`.

**Audit Trail Tables**: Critical entities have companion audit tables. The `parcel_status_history` table records every status transition with the previous status, new status, actor, location, and timestamp — providing a complete, immutable audit trail for compliance and debugging.

**Soft State Management**: Instead of deleting records, many tables use status columns (e.g., `status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'`) and boolean flags (e.g., `is_active BOOLEAN NOT NULL DEFAULT TRUE`) to manage entity lifecycle, preserving historical data.

### 6.3.3 Data Storage by Service

**Identity Service (`swiftdrop_identity`)**:
Stores the complete user identity graph including:
- `users` — Core user profiles with email, password hash (BCrypt), full name, and phone number.
- `roles` — System roles (`CUSTOMER`, `MERCHANT`, `COURIER`, `ADMIN`).
- `user_roles` — Many-to-many mapping between users and roles.
- `oauth_identities` — External OAuth provider linkages (Google OpenID Connect).
- `refresh_sessions` — JWT refresh token sessions with IP address, user agent, and revocation tracking.
- `password_reset_tokens` — One-time password reset tokens with expiration.
- `merchant_approval_records` — Pending and completed merchant onboarding applications.
- `account_settings` — User-specific notification and display preferences.

**Parcel Service (`swiftdrop_parcel`)**:
Stores the parcel lifecycle data including:
- `parcels` — Complete parcel records with sender/recipient details, pickup/delivery addresses, dimensions (length, width, height), weight, delivery type, status, and delivery fee.
- `parcel_status_history` — Immutable audit trail of every status change.
- `persistent_estimates` — Cached delivery time and cost estimates.
- Merchant order references and COD (Cash on Delivery) fields.

**Merchant Service (`swiftdrop_merchant`)**:
Stores merchant-specific business data including:
- Merchant profiles with business information.
- Integration contracts defining API access and service-level agreements.

**Dispatch Service (`swiftdrop_dispatch`)**:
Stores courier and assignment data including:
- `couriers` — Courier profiles with vehicle type, license plate, assigned delivery zone, availability status, and concurrent delivery limits.
- `dispatch_assignments` — Parcel-to-courier assignments with pickup/delivery addresses, status, and completion timestamps.
- `proof_of_delivery` — Delivery confirmation records including recipient NIC, signature URL, photo URL, and delivery notes.
- Notification recipient mappings and concurrent assignment tracking.

**Pricing Service (`swiftdrop_pricing`)**:
Stores the pricing engine configuration including:
- `delivery_zones` — Geographic zones with zone codes and base multipliers.
- `pricing_rules` — Service-type-specific rules (STANDARD, EXPRESS, SAME_DAY) with base fare, weight rates, distance rates, and urgency multipliers.
- `merchant_tier_discounts` — Volume-based discount tiers (BRONZE, SILVER, GOLD, PLATINUM).
- `promo_codes` — Time-bound promotional codes with usage limits and discount caps.
- `promo_redemptions` — Audit trail of promo code usage for idempotency.
- `fuel_surcharge_index` — Dynamic fuel cost adjustment rates.

**Tracking Service (`swiftdrop_tracking`)**:
Stores real-time location and movement data including:
- GPS coordinates and timestamped location updates.
- Movement audit trail for each parcel's journey.

**Notification Service (`swiftdrop_notification`)**:
Stores notification delivery records including:
- Notification templates and channel configurations.
- Delivery records tracking sent notifications.
- Read state tracking per user per notification.
- Email preference settings.

**AI Assistant Service (`swiftdrop_reports`)**:
Stores analytics and reporting data including:
- Generated report records.
- AI-assisted insights and query results.

### 6.3.4 Data Persistence Strategy

SwiftDrop follows a layered persistence strategy:

1. **Primary Storage**: All business-critical data is stored in PostgreSQL relational tables with full ACID guarantees.
2. **Event-Driven Replication**: When a state change occurs (e.g., a parcel status updates), the owning service publishes an event to RabbitMQ. Downstream services consume the event and update their own local data stores, achieving eventual consistency without direct database coupling.
3. **Connection Pooling**: HikariCP manages a pool of JDBC connections per microservice, ensuring efficient connection reuse and preventing connection exhaustion under load.
4. **Docker Volume Persistence**: The PostgreSQL data directory is mounted to a Docker named volume (`postgres_data`), ensuring that data survives container restarts and rebuilds.

### 6.3.5 Security Considerations

- **Password Storage**: User passwords are never stored in plaintext. They are hashed using BCrypt with a configurable work factor before being written to the `password_hash` column.
- **Token Hashing**: JWT refresh tokens are SHA-256 hashed before storage in the `refresh_sessions` table, preventing token recovery even if the database is compromised.
- **Database Access**: Each microservice connects to its own database using environment-variable-injected credentials. No service has access to another service's database.
- **SQL Injection Prevention**: All queries are executed through Hibernate's parameterized query API or Spring Data JPA's type-safe repository methods, eliminating SQL injection vulnerabilities.

### 6.3.6 Scalability Considerations

While the current deployment uses a single PostgreSQL instance, the architecture is designed for future horizontal scaling:

- **Database-per-Service**: Each service's database can be migrated to its own dedicated PostgreSQL server or to a managed cloud database (e.g., Amazon RDS, Google Cloud SQL) independently.
- **Read Replicas**: PostgreSQL supports streaming replication, enabling read replicas for analytics-heavy queries (e.g., reporting dashboards).
- **Partitioning**: Large tables like `parcel_status_history` and tracking location data can be partitioned by date range to maintain query performance as data grows.
- **Connection Pool Tuning**: HikariCP parameters (maximum pool size, minimum idle, connection timeout) can be tuned per service based on expected concurrency patterns.

---

*End of Chapter 06*
