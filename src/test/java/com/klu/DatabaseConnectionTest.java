package com.klu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testDatabaseConnection() {
        assertNotNull(dataSource, "DataSource should not be null");
        
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Connection should not be null");
            assertFalse(connection.isClosed(), "Connection should be open");
            System.out.println("✅ Database connection successful!");
            System.out.println("Connected to: " + connection.getMetaData().getURL());
        } catch (Exception e) {
            fail("Failed to connect to database: " + e.getMessage());
        }
    }

    @Test
    void testUsersTableExists() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users", 
                Integer.class
            );
            assertNotNull(count);
            System.out.println("✅ Users table exists. Record count: " + count);
        } catch (Exception e) {
            fail("Users table not found: " + e.getMessage());
        }
    }

    @Test
    void testProductsTableExists() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products", 
                Integer.class
            );
            assertNotNull(count);
            System.out.println("✅ Products table exists. Record count: " + count);
        } catch (Exception e) {
            fail("Products table not found: " + e.getMessage());
        }
    }

    @Test
    void testFlywayMigrationsApplied() {
        // Flyway migrations are applied during startup - we can verify by checking if tables exist
        try {
            Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users", 
                Integer.class
            );
            Integer productCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products", 
                Integer.class
            );
            assertNotNull(userCount);
            assertNotNull(productCount);
            System.out.println("✅ Flyway migrations applied successfully!");
            System.out.println("   - Users: " + userCount + " record(s)");
            System.out.println("   - Products: " + productCount + " record(s)");
        } catch (Exception e) {
            fail("Flyway migrations verification failed: " + e.getMessage());
        }
    }
}
