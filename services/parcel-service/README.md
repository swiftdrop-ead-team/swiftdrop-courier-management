# Parcel Service

## Assigned Member: Member 3
**Branch**: `feature/parcel-service`  
**Port**: `8082`  
**Status**: Reserved (Future Phase)  
**Database**: `swiftdrop_parcel`

## Responsibilities
- Parcel registration, dimension/weight capture, and label generation.
- Order intake from Customers and Merchants.
- State machine for parcel lifecycle (`CREATED`, `RECEIVED_AT_HUB`, `SORTED`, `IN_TRANSIT`, `OUT_FOR_DELIVERY`, `DELIVERED`, `FAILED`).
- Publishing domain events to RabbitMQ:
  - `parcel.created`
  - `parcel.status_updated`
  - `parcel.cancelled`

## Dependencies
- Consumes JWT claims passed from API Gateway for authorization.
- Integrates with Pricing Service for delivery cost calculation.
- Emits events consumed by Notification and Tracking Services.
