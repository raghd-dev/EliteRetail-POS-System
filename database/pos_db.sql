-- EliteRetail POS System database schema
-- MySQL 8.0+
-- DEVELOPMENT / FRESH-CLONE SETUP: this script drops the EliteRetail tables in pos_db.
-- Back up an existing database before running it against real data.

CREATE DATABASE IF NOT EXISTS pos_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE pos_db;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS bill_items;
DROP TABLE IF EXISTS bills;
DROP TABLE IF EXISTS discounts;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    cashier_name VARCHAR(100) NOT NULL,
    cashier_code VARCHAR(50) NOT NULL,
    password VARCHAR(512) NOT NULL,
    role ENUM('MANAGER', 'CASHIER') NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_cashier_code (cashier_code)
) ENGINE=InnoDB;

CREATE TABLE product (
    code VARCHAR(32) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    category VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (code),
    KEY idx_product_name (name),
    KEY idx_product_category (category),
    CONSTRAINT chk_product_price CHECK (unit_price >= 0),
    CONSTRAINT chk_product_stock CHECK (stock_quantity >= 0)
) ENGINE=InnoDB;

CREATE TABLE discounts (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500) NOT NULL DEFAULT '',
    value DECIMAL(12,2) NOT NULL DEFAULT 0,
    type ENUM('PERCENT', 'FIXED_AMOUNT', 'BUY_X_GET_Y') NOT NULL,
    apply_to VARCHAR(150) NOT NULL DEFAULT 'ALL',
    target_value VARCHAR(50) NOT NULL DEFAULT '',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    min_quantity INT NOT NULL DEFAULT 1,
    min_purchase_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    usage_limit INT NOT NULL DEFAULT 0,
    times_used INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_discounts_active_dates (active, start_date, end_date),
    CONSTRAINT chk_discount_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_discount_quantity CHECK (min_quantity >= 0),
    CONSTRAINT chk_discount_usage CHECK (usage_limit >= 0 AND times_used >= 0)
) ENGINE=InnoDB;

CREATE TABLE bills (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    bill_number VARCHAR(64) NOT NULL,
    machine_number VARCHAR(32) NOT NULL,
    serial_number INT NOT NULL,
    cashier_code VARCHAR(50) NOT NULL,
    cashier_name VARCHAR(100) NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    total_discount DECIMAL(12,2) NOT NULL DEFAULT 0,
    payment_method ENUM('CASH', 'CREDIT_CARD', 'DEBIT_CARD', 'MOBILE_PAY') NOT NULL,
    amount_received DECIMAL(12,2) NOT NULL DEFAULT 0,
    change_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_bills_bill_number (bill_number),
    KEY idx_bills_created_at (created_at),
    KEY idx_bills_cashier_code (cashier_code),
    CONSTRAINT fk_bills_cashier
        FOREIGN KEY (cashier_code) REFERENCES users(cashier_code)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_bills_total CHECK (total >= 0),
    CONSTRAINT chk_bills_discount CHECK (total_discount >= 0)
) ENGINE=InnoDB;

CREATE TABLE bill_items (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    bill_id BIGINT UNSIGNED NOT NULL,
    product_code VARCHAR(32) NOT NULL,
    product_name VARCHAR(150) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    discount DECIMAL(12,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_bill_items_bill_id (bill_id),
    KEY idx_bill_items_product_code (product_code),
    CONSTRAINT fk_bill_items_bill
        FOREIGN KEY (bill_id) REFERENCES bills(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_bill_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_bill_items_price CHECK (unit_price >= 0),
    CONSTRAINT chk_bill_items_discount CHECK (discount >= 0)
) ENGINE=InnoDB;

-- Demo users. The application also accepts legacy plaintext passwords for migration,
-- but new/public sample data uses PBKDF2-HMAC-SHA256 hashes.
-- Manager login: manager001 / manager123
-- Cashier login: cashier001 / cashier123
INSERT INTO users (cashier_name, cashier_code, password, role) VALUES
('Demo Manager', 'manager001', 'pbkdf2$210000$iFuLQ3mYqoCF0q6rfaULjg==$5fgLTNAbv9Im81SFII4UCdYOW0FABO7ZL09XssZwV50=', 'MANAGER'),
('Demo Cashier', 'cashier001', 'pbkdf2$210000$Zm36GX6au3SATwwstYitEg==$lsMDSJj02lWsBN/eUum/YiwhQXgShTSiR1koIIWHV78=', 'CASHIER');

INSERT INTO product (code, name, description, unit_price, stock_quantity, category) VALUES
('10000001', 'Fresh Milk 1L', 'Whole milk, 1 litre', 2.50, 50, 'Dairy & Eggs'),
('20000001', 'White Bread', 'Fresh sliced white bread', 1.80, 40, 'Bakery & Bread'),
('30000001', 'Orange Juice 1L', '100% orange juice', 3.25, 35, 'Beverages'),
('40000001', 'Potato Chips', 'Classic salted potato chips', 1.50, 60, 'Snacks & Chips');

INSERT INTO discounts
(name, description, value, type, apply_to, target_value, start_date, end_date,
 min_quantity, min_purchase_amount, active, usage_limit, times_used)
VALUES
('10% Store Promotion', 'Ten percent off all products', 10.00, 'PERCENT', 'ALL', '', '2025-01-01', '2030-12-31', 1, 0, TRUE, 0, 0),
('Bread Buy 2 Get 1', 'Buy two White Bread items and get one free', 0.00, 'BUY_X_GET_Y', 'White Bread', '2+1', '2025-01-01', '2030-12-31', 3, 0, TRUE, 0, 0);
