-- Runs automatically ONCE, the first time the MySQL container starts with an empty data volume.
-- Creates one isolated database per microservice.
CREATE DATABASE IF NOT EXISTS user_service_db;
CREATE DATABASE IF NOT EXISTS order_service_db;
CREATE DATABASE IF NOT EXISTS notification_service_db;
CREATE DATABASE IF NOT EXISTS audit_service_db;
