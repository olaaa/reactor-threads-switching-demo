-- src/main/resources/schema.sql
CREATE TABLE IF NOT EXISTS "users"
(
    id    IDENTITY PRIMARY KEY,
    name  VARCHAR(255),
    email VARCHAR(255)
);