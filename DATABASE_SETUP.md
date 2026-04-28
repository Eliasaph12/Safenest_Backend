# Database Setup Guide

## 1. User and Database Creation

Run the following SQL in MySQL Workbench to create the database and the dedicated user:

```sql
CREATE DATABASE IF NOT EXISTS safenest_db;
CREATE USER IF NOT EXISTS 'safenest_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON safenest_db.* TO 'safenest_user'@'localhost';
FLUSH PRIVILEGES;

USE safenest_db;

-- Create the products table for safety resources
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2),
    stock_quantity INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 2. Initialize Backend

```bash
cd SafeNest-backend
npm install
npm start
```

## 3. Verify Connection

### Method A: API Health Check
Start your backend and visit:
`http://localhost:8080/health`

You should see:
`{"status":"OK","message":"MySQL connected"}`

### Method B: Manual Verification
Run these commands in MySQL Workbench to verify that CRUD (Create, Read, Update, Delete) is fully functional:

```sql
USE safenest_db;

-- Check if table exists
DESCRIBE products;

-- 1. Create (Insert)
INSERT INTO products (name, description, price, stock_quantity) 
VALUES ('Emergency Kit', 'Standard survival kit', 25.00, 10);

-- 2. Read (Select)
SELECT * FROM products;

-- 3. Update (Modify) - Testing "Modification of data"
UPDATE products SET price = 20.00 WHERE name = 'Emergency Kit';

-- 4. Delete (Remove)
DELETE FROM products WHERE name = 'Emergency Kit';
```

## Troubleshooting

### Connection Timeout
- Check if Railway database is running
- Verify network access (firewall, IP whitelist)

### Table Not Found
If the backend logs show "Table products doesn't exist", re-run the SQL script in Section 1.

### Deployment Notes (Render)
Render does not offer a managed MySQL service. To use Render for the backend, you must host your MySQL database externally (e.g., via Railway or Aiven) and provide the connection credentials to Render via Environment Variables.
