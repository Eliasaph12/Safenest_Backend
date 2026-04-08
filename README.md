# E-Commerce Backend

This is the backend for the E-Commerce application, built with Spring Boot.

## Prerequisites

- Java 17
- Maven
- MySQL

## Setup

1. Install Java 17, Maven, and MySQL if not already installed.
2. Create a MySQL database: `CREATE DATABASE ecommerce_db;`
3. Update `src/main/resources/application.properties` with your MySQL password.
4. Navigate to the backend directory.
5. Run `mvn clean install` to build the project.
6. Run `mvn spring-boot:run` to start the server on port 8081.

## API Endpoints

- POST /api/auth/signup - Register a new user
- POST /api/auth/login - Login user
- GET /api/products - Get all products
- POST /api/products - Add a new product
- GET /api/products/{id} - Get product by ID
- PUT /api/products/{id} - Update product
- DELETE /api/products/{id} - Delete product

CORS enabled for http://localhost:5173