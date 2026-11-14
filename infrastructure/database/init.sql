-- ==============================================================================
-- SwiftDrop — Database Initialization Script
-- Executed on fresh PostgreSQL container startup
-- ==============================================================================

-- Create microservices databases
SELECT 'CREATE DATABASE swiftdrop_identity'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'swiftdrop_identity')
\gexec
SELECT 'CREATE DATABASE swiftdrop_parcel'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'swiftdrop_parcel')
\gexec
SELECT 'CREATE DATABASE swiftdrop_merchant'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'swiftdrop_merchant')
\gexec
SELECT 'CREATE DATABASE swiftdrop_dispatch'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'swiftdrop_dispatch')
\gexec
SELECT 'CREATE DATABASE swiftdrop_pricing'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'swiftdrop_pricing')
\gexec
SELECT 'CREATE DATABASE swiftdrop_tracking'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'swiftdrop_tracking')
\gexec
SELECT 'CREATE DATABASE swiftdrop_notification'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'swiftdrop_notification')
\gexec

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_identity TO swiftdrop_user;
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_parcel TO swiftdrop_user;
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_merchant TO swiftdrop_user;
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_dispatch TO swiftdrop_user;
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_pricing TO swiftdrop_user;
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_tracking TO swiftdrop_user;
GRANT ALL PRIVILEGES ON DATABASE swiftdrop_notification TO swiftdrop_user;
