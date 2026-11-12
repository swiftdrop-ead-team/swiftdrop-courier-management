# Merchant Service

## Assigned Member: Member 4
**Branch**: `feature/merchant-service`
**Port**: `8083`
**Database**: `swiftdrop_merchant`
**Stack**: Java 21, Spring Boot 3.4.3, Spring Data JPA, Flyway, RabbitMQ

## Responsibilities
- Merchant business profile registration and service-owned approval.
- Scoped API keys, idempotent order intake, order status dashboard, and signed webhooks.
- Delivery audit, durable retry scheduling, and manual redelivery.

## Configuration

Set these environment variables when running the service:

| Variable | Required | Purpose |
|---|---:|---|
| `JWT_SECRET` | Yes | Base64 HMAC key shared with Identity Service |
| `SPRING_DATASOURCE_URL` | Yes | JDBC URL for `swiftdrop_merchant` |
| `SPRING_DATASOURCE_USERNAME` | Yes | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Database password |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | Yes | Broker connection |
| `WEBHOOK_ENCRYPTION_KEY` | Yes for webhook create/update | At least 32 characters; encrypts signing secrets at rest |
| `PARCEL_SERVICE_URL` | Yes for order intake | Parcel service base URL |
| `PARCEL_ORDER_PATH` | Confirm with Member 3 | Parcel order endpoint path |
| `WEBHOOK_ALLOW_LOCALHOST` | No | Defaults to `false`; enable only for a local demo receiver |

Webhook deliveries use HTTPS only by default, reject credentials and non-public DNS/IP targets, and do not follow redirects. Localhost/HTTP is allowed only when `WEBHOOK_ALLOW_LOCALHOST=true` for a controlled demo.

## Registration and approval

The user must first register with Identity Service and call Merchant Service with the resulting Bearer JWT and `ROLE_MERCHANT`. The Merchant Service creates a local profile in `PENDING` state. `ROLE_ADMIN` reviews it at the Merchant admin endpoints. Approved status gates API-key issuance, orders, dashboard, and webhook management.

Identity Service still has its own merchant approval workflow. This service does not access the Identity database; the service-local approval does not automatically update Identity. Coordinate that transition with Member 2 before relying on a single approval action for sign-in and all system views.

## Run and test

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

The JaCoCo HTML report is generated at `target/site/jacoco/index.html`.

Run the dependencies and Merchant Service from PowerShell at the repository root:

```powershell
docker compose --env-file .env.example up -d postgres rabbitmq

$settings = @{}
Get-Content .env.example | ForEach-Object {
    if ($_ -match '^(POSTGRES_USER|POSTGRES_PASSWORD|RABBITMQ_USERNAME|RABBITMQ_PASSWORD|JWT_SECRET)=(.*)$') {
        $settings[$Matches[1]] = $Matches[2]
    }
}

$env:JWT_SECRET = $settings["JWT_SECRET"]
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5433/swiftdrop_merchant"
$env:SPRING_DATASOURCE_USERNAME = $settings["POSTGRES_USER"]
$env:SPRING_DATASOURCE_PASSWORD = $settings["POSTGRES_PASSWORD"]
$env:RABBITMQ_HOST = "localhost"
$env:RABBITMQ_PORT = "5672"
$env:RABBITMQ_USERNAME = $settings["RABBITMQ_USERNAME"]
$env:RABBITMQ_PASSWORD = $settings["RABBITMQ_PASSWORD"]

$webhookKey = [Environment]::GetEnvironmentVariable("WEBHOOK_ENCRYPTION_KEY", "User")
if ([string]::IsNullOrWhiteSpace($webhookKey)) {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $rng.Dispose()
    $webhookKey = [Convert]::ToBase64String($bytes)
    [Environment]::SetEnvironmentVariable("WEBHOOK_ENCRYPTION_KEY", $webhookKey, "User")
}
$env:WEBHOOK_ENCRYPTION_KEY = $webhookKey

Set-Location services\merchant-service
.\mvnw.cmd spring-boot:run
```

Swagger UI: `http://localhost:8083/swagger-ui.html`
Health: `http://localhost:8083/actuator/health`

See [the v1 API contract](../../docs/api-contracts/merchant-api-v1.md) and [the Postman collection](SwiftDrop_Merchant_Service.postman_collection.json).
