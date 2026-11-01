# Notification Service

## Assigned Member: Member 8
**Branch**: `feature/notification-service`  
**Port**: `8087`  
**Status**: Reserved (Future Phase)  
**Database**: `swiftdrop_notification`

## Responsibilities
- Multi-channel notification delivery: Email (SMTP / SendGrid), SMS (Twilio), Push notifications.
- Consumes RabbitMQ domain events (`parcel.status_updated`, `merchant.approved`, `courier.assigned`).
- User preference management (opt-in/opt-out channels).
- Delivery receipt and proof-of-delivery notification dispatch.
