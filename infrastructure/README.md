# SwiftDrop — Infrastructure & Persistence

## Overview
This directory manages core infrastructure configurations, database initialization scripts, and shared messaging definitions for SwiftDrop.

## Managed Resources
- **PostgreSQL 16**: Primary relational database cluster.
- **Database per Service**: In compliance with microservices isolation patterns, each microservice maintains a dedicated logical database:
  - `swiftdrop_identity` (Implemented - Member 2)
  - `swiftdrop_parcel` (Reserved - Member 3)
  - `swiftdrop_merchant` (Reserved - Member 4)
  - `swiftdrop_dispatch` (Reserved - Member 5)
  - `swiftdrop_pricing` (Reserved - Member 6)
  - `swiftdrop_tracking` (Reserved - Member 7)
  - `swiftdrop_notification` (Reserved - Member 8)

## Local Execution
To start PostgreSQL independently:
```bash
docker compose up -d postgres
```
To connect via `psql`:
```bash
docker exec -it swiftdrop-postgres psql -U swiftdrop_user -d swiftdrop_identity
```
