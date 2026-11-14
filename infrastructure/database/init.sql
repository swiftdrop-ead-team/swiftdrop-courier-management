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

-- Grant privileges to the configured PostgreSQL initialization user.
SELECT format('GRANT ALL PRIVILEGES ON DATABASE %I TO %I', datname, current_user)
FROM pg_database WHERE datname LIKE 'swiftdrop_%'
\gexec
