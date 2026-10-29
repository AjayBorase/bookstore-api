-- PostgreSQL schema for the Bookstore Management REST API
-- Usage:  createdb bookstore && psql -d bookstore -f database/schema.sql

CREATE TABLE IF NOT EXISTS customers (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,                       -- BCrypt hash
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'CUSTOMER')),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS books (
    id        BIGSERIAL PRIMARY KEY,
    title     VARCHAR(255)   NOT NULL,
    author    VARCHAR(255)   NOT NULL,
    isbn      VARCHAR(20)    NOT NULL UNIQUE,
    price     NUMERIC(10, 2) NOT NULL CHECK (price > 0),
    stock     INTEGER        NOT NULL CHECK (stock >= 0),
    category  VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS orders (
    id            BIGSERIAL PRIMARY KEY,
    customer_id   BIGINT         NOT NULL REFERENCES customers (id),
    status        VARCHAR(20)    NOT NULL CHECK (status IN ('PLACED', 'CANCELLED')),
    total_amount  NUMERIC(12, 2) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    book_id     BIGINT         NOT NULL REFERENCES books (id),
    quantity    INTEGER        NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(10, 2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_customer     ON orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status       ON orders (status);
CREATE INDEX IF NOT EXISTS idx_order_items_order   ON order_items (order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_book    ON order_items (book_id);
CREATE INDEX IF NOT EXISTS idx_books_category      ON books (category);
