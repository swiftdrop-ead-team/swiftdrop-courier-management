-- ==============================================================================
-- SwiftDrop — Database Initialization Script
-- Executed on fresh PostgreSQL container startup
-- ==============================================================================

-- Create microservices databases
CREATE DATABASE swiftdrop_identity;
CREATE DATABASE swiftdrop_parcel;
CREATE DATABASE swiftdrop_merchant;
CREATE DATABASE swiftdrop_dispatch;
CREATE DATABASE swiftdrop_pricing;
CREATE DATABASE swiftdrop_tracking;
CREATE DATABASE swiftdrop_notification;
CREATE DATABASE swiftdrop_reports;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_identity TO swiftdrop_user;
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_parcel TO swiftdrop_user;
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_merchant TO swiftdrop_user;
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_dispatch TO swiftdrop_user;
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_pricing TO swiftdrop_user;
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_tracking TO swiftdrop_user;
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_notification TO swiftdrop_user;
