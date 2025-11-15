-- PostgreSQL initialization script for Order Platform
-- This script creates the keycloak database for Keycloak to use

-- Create keycloak database
CREATE DATABASE keycloak;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE keycloak TO orderuser;

-- Note: The orderplatform database is created by POSTGRES_DB environment variable
-- Application-specific tables (events, snapshots, etc.) will be created by Flyway migrations
