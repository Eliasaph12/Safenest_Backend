# Database Setup Guide

## 1. Environment Variables Setup

Copy `.env.example` to `.env` and update your values.

### Example values:
```
DATABASE_URL=jdbc:mysql://localhost:3306/your_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DATABASE_USER=your_db_user
DATABASE_PASSWORD=your_db_password
SERVER_PORT=8080
FRONTEND_URL=http://localhost:3000
```

**For Railway:**
- Add MySQL database plugin/add-on
- Copy DATABASE_URL/USER/PASSWORD from Railway dashboard Variables tab
- Set SPRING_PROFILES_ACTIVE=prod (optional, via Procfile)

### Windows (PowerShell)
```powershell
$env:DATABASE_URL="jdbc:mysql://your-host:3306/yourdb?..."
# ... etc from .env
```

### Windows (Command Prompt)
```cmd
set DATABASE_URL=jdbc:mysql://your-host:3306/yourdb?...
```

### Linux/Mac (Bash)
```bash
export DATABASE_URL="jdbc:mysql://your-host:3306/yourdb?..."
```

## 2. Build the Project

```bash
mvn clean compile
```

This will download Flyway dependencies and compile your project.

## 3. Run Database Migrations

First time you run the application:

```bash
mvn spring-boot:run
```

Flyway will automatically:
- Create the `flyway_schema_history` table
- Execute `V1__Initial_Schema.sql` (creates tables)
- Execute `V2__Insert_Sample_Data.sql` (inserts sample data)

## 4. Test Database Connection

Run the database connection tests:

```bash
mvn test -Dtest=DatabaseConnectionTest
```

Expected output:
```
✅ Database connection successful!
Connected to: jdbc:h2:mem:testdb (test environment)
✅ Users table exists. Record count: 0 (ready for API data)
✅ Products table exists. Record count: 5 (with sample products)
✅ Flyway migrations applied successfully!
   - Users: 0 record(s)
   - Products: 5 record(s)
```

**Note:** For local/test environments, we use H2 in-memory database. When connecting to Railway MySQL in production, environment variables route to the cloud database instead.

## 5. What Changed

### Added to pom.xml
- `flyway-core` - Flyway core library
- `flyway-mysql` - MySQL dialect support

### Updated Configuration
- `application.properties` - Now uses environment variables for Railway database
- New Flyway configuration added to validate schema on startup

### Created Files
- `db/migration/V1__Initial_Schema.sql` - Database schema (tables: users, products, orders, order_items)
- `db/migration/V2__Insert_Sample_Data.sql` - Sample data for testing
- `DatabaseConnectionTest.java` - Tests to verify connection and schema

## 6. Adding New Migrations

To add new migrations, create new SQL files in `src/main/resources/db/migration/`:

Format: `V<VERSION>__<DESCRIPTION>.sql`

Examples:
- `V3__Add_User_Phone.sql`
- `V4__Create_Reviews_Table.sql`

Flyway will execute them in version order on next startup.

## 7. Security Notes

⚠️ **DO NOT commit credentials to Git!**

1. Create a `.env` file (local, not tracked):
   ```
   Copy .env.example to .env
   Update passwords for your environment
   ```

2. Add to `.gitignore`:
   ```
   .env
   .env.local
   .env.*.local
   ```

3. For production (Railway, Docker, etc.):
   - Use Railway's built-in secrets manager
   - Set environment variables in deployment platform
   - Never hardcode credentials in code

## 8. Connection Details

Your Railway database configuration is now properly:
- ✅ Using environment variables (not hardcoded)
- ✅ Using SSL/TLS encryption (useSSL=true)
- ✅ Set with UTC timezone handling
- ✅ Validated on every startup with Flyway

## Troubleshooting

### Connection Timeout
- Check if Railway database is running
- Verify network access (firewall, IP whitelist)
- Test with: `mvn test -Dtest=DatabaseConnectionTest`

### Flyway Migration Failed
- Check SQL syntax in migration files
- Ensure proper naming format: `V<NUMBER>__<DESC>.sql`
- Check database permissions (user should have CREATE, ALTER, DROP)
- View logs: `tail -f logs/safenest.log`

### Table Already Exists
- Flyway prevents duplicate runs automatically
- Clean start: `mvn flyway:clean` (⚠️ warning: deletes schema!)
