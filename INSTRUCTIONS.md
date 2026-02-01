# Archiving System - Backend Instructions

## Overview

This is a **Spring Modulith** application for managing digital archives with support for multiple international archiving standards (NOARK5, OAIS, PREMIS, Dublin Core, METS, EAD, BagIt, ISAD(G), MODS).

## Tech Stack

- **Framework**: Spring Boot 3.5.4
- **Java**: 21
- **Architecture**: Spring Modulith (modular monolith)
- **API**: GraphQL
- **Database**: PostgreSQL (production), H2 (development)
- **ORM**: JPA/Hibernate
- **Build Tool**: Maven

## Prerequisites

- **Java 21** or higher
- **Maven 3.8+**
- **PostgreSQL** (for production)
- **Docker & Docker Compose** (optional, for containerized database)

## Project Structure

```
archiving/
├── src/main/java/com/dmc/archiving/
│   ├── user/                    # User module
│   │   ├── api/                 # Public API (exposed to other modules)
│   │   ├── model/               # User entity
│   │   ├── repository/          # User repository
│   │   └── service/             # User service
│   ├── tenancy/                 # Tenancy module
│   │   ├── api/                 # Public API
│   │   ├── events/              # Event listeners
│   │   ├── model/               # Tenant entity & settings
│   │   ├── repository/          # Tenant repository
│   │   └── service/             # Tenant service
│   ├── archive/                 # Archive module
│   │   ├── model/               # Archive, Element entities
│   │   ├── repository/          # Archive repositories
│   │   ├── service/             # Archive service
│   │   └── strategy/            # Strategy pattern for standards
│   │       └── impl/            # Standard implementations
│   └── ArchivingApplication.java
├── src/main/resources/
│   ├── application.properties
│   ├── application-compose.properties
│   ├── application-dev-reset.properties
│   ├── application-docker.properties
│   ├── data.sql
│   └── graphql/schema.graphqls
├── pom.xml
├── compose.yaml
└── Dockerfile
```

## Quick Start

### 1. Using Docker Compose (Recommended)

```bash
# Start PostgreSQL database
docker-compose up -d

# Run the application
mvn spring-boot:run -Dspring-boot.run.profiles=compose
```

The application will be available at:
- **GraphQL API**: http://localhost:2020/graphql
- **GraphQL Playground**: http://localhost:2020/graphiql

### 2. Using Embedded H2 Database

```bash
# Run with H2 (no external database needed)
mvn spring-boot:run

# Access H2 Console at http://localhost:2020/h2-console
# JDBC URL: jdbc:h2:mem:testdb
# Username: sa
# Password: (leave empty)
```

### 3. Production Build

```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/archiving-0.0.1-SNAPSHOT.jar
```

## Spring Modulith Architecture

### Module Boundaries

The application follows Spring Modulith principles with strict module boundaries:

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│    User     │────▶│   Tenancy    │────▶│   Archive   │
│   Module    │     │    Module    │     │   Module    │
└─────────────┘     └──────────────┘     └─────────────┘
      │                    │                     │
      └────────────────────┴─────────────────────┘
                    Events (loose coupling)
```

### Module Communication

**✅ Allowed:**
- Modules use **public APIs** (classes in `api` package)
- Modules publish/subscribe to **events**
- Modules depend on **shared models** from API packages

**❌ Not Allowed:**
- Direct dependency on another module's service
- Access to another module's repository
- Importing classes outside the `api` package

### Example: User Deletion Flow

```java
// User module publishes event
eventPublisher.publishEvent(new UserDeletedEvent(this, userId));

// Tenancy module listens to event
@EventListener
public void handleUserDeleted(UserDeletedEvent event) {
    tenancyService.removeUserFromTenant(event.getUserId());
}
```

## Archiving Standards (Strategy Pattern)

The application supports 9 archiving standards using the Strategy Pattern:

### Available Strategies

| Standard | Implementation | Focus |
|----------|---------------|--------|
| NOARK5 | `Noark5Strategy` | Norwegian electronic archives |
| OAIS | `OaisStrategy` | Digital preservation (ISO 14721) |
| PREMIS | `PremisStrategy` | Preservation metadata |
| Dublin Core | `DublinCoreStrategy` | Simple metadata (15 elements) |
| METS | `MetsStrategy` | Structural metadata |
| EAD | `EadStrategy` | Finding aids |
| BagIt | `BagitStrategy` | File packaging (RFC 8493) |
| ISAD(G) | `IsadgStrategy` | International archival description |
| MODS | `ModsStrategy` | Bibliographic metadata |

### Using Strategies

```java
// Get strategy for an archive
ArchiveStrategy strategy = strategyFactory.getStrategy(archive.getStandard());

// Validate archive
ValidationResult result = strategy.validate(archive);

// Export in standard-specific format
Map<String, Object> exportData = strategy.export(archive);

// Get metadata requirements
Map<String, String> requirements = strategy.getMetadataRequirements();
```

### Adding a New Standard

1. Create strategy class:
```java
@Component
public class NewStandardStrategy extends AbstractArchiveStrategy {
    @Override
    public String getStandardName() {
        return "NEW_STANDARD";
    }
    
    @Override
    protected void validateStandard(Archive archive, ValidationResult result) {
        // Standard-specific validation
    }
    
    @Override
    protected void exportStandard(Archive archive, Map<String, Object> exportData) {
        // Standard-specific export format
    }
    
    // ... implement other methods
}
```

2. Register in factory constructor
3. Add to GraphQL enum in `schema.graphqls`
4. Add to Java enum in `ArchiveStandard.java`

## GraphQL API

### Schema Location

`src/main/resources/graphql/schema.graphqls`

### Common Queries

```graphql
# Get all users
query {
  getAllUsers {
    id
    name
    email
  }
}

# Get all archives
query {
  getAllArchives {
    id
    title
    standard
    status
    createdAt
  }
}

# Get all tenants
query {
  getAllTenants {
    id
    name
    domain
    status
  }
}
```

### Common Mutations

```graphql
# Create user
mutation {
  createUser(input: {
    name: "John Doe"
    email: "john@example.com"
    age: 30
  }) {
    id
    name
  }
}

# Create archive
mutation {
  createArchive(input: {
    userId: 1
    title: "My Archive"
    description: "Test archive"
    content: "{}"
    standard: NOARK5
  }) {
    id
    title
    standard
  }
}

# Delete user (triggers event to clean up tenant associations)
mutation {
  deleteUser(id: 1)
}
```

### Testing with GraphQL Playground

1. Navigate to http://localhost:2020/graphiql
2. Use the schema explorer on the right
3. Run queries and mutations
4. View responses and errors

## REST Endpoints

### Archive Operations

```bash
# Extract archive (download)
POST /api/archives/{archiveId}/extract
Content-Type: application/json
{"password": "user_password"}

# Validate archive against standard
POST /api/archives/{archiveId}/validate

# Get metadata requirements for a standard
GET /api/standards/{standardName}/requirements
```

### Example: Extract Archive

```bash
curl -X POST http://localhost:2020/api/archives/1/extract \
  -H "Content-Type: application/json" \
  -d '{"password":"test123"}' \
  -o archive_export.json
```

## Database

### Profiles

- **default**: H2 in-memory database
- **compose**: PostgreSQL via Docker Compose
- **docker**: PostgreSQL in Docker
- **dev-reset**: H2 with schema reset on startup

### Schema

Main tables:
- `users` - User accounts
- `tenants` - Tenant organizations
- `user_tenant` - Many-to-many relationship
- `archives` - Archive records
- `elements` - Hierarchical archive elements
- `user_assignments` - User-archive assignments

### Data Initialization

Sample data is loaded from `src/main/resources/data.sql` on startup.

### Migrations

Currently using JPA auto-DDL. For production, consider:
- Flyway: https://flywaydb.org/
- Liquibase: https://www.liquibase.org/

## Configuration

### Application Properties

```properties
# Server
server.port=2020

# Database (compose profile)
spring.datasource.url=jdbc:postgresql://localhost:5432/archiving
spring.datasource.username=user
spring.datasource.password=password

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# GraphQL
spring.graphql.graphiql.enabled=true
spring.graphql.path=/graphql
```

### Environment Variables

```bash
# Override application properties
export SPRING_PROFILES_ACTIVE=compose
export SERVER_PORT=8080
export DB_URL=jdbc:postgresql://localhost:5432/archiving
export DB_USERNAME=user
export DB_PASSWORD=password
```

## Development

### Hot Reload

Spring Boot DevTools is included for automatic restart:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
</dependency>
```

Changes to Java files trigger automatic restart.

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=ArchivingApplicationTests

# Run with coverage
mvn test jacoco:report
```

### Spring Modulith Tests

```bash
# Verify module structure
mvn test -Dtest=ModulithStructureTest

# Generate module documentation
mvn spring-modulith:docs
# Output: target/spring-modulith-docs/
```

### Logging

```bash
# Enable debug logging
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.dmc.archiving=DEBUG"

# Log SQL queries
-Dspring.jpa.show-sql=true
-Dlogging.level.org.hibernate.SQL=DEBUG
```

## Troubleshooting

### Circular Dependency Error

**Issue**: `The dependencies of some of the beans form a cycle`

**Solution**: The application uses events to break circular dependencies. Ensure:
- Events are in the `api` package
- Modules use `@EventListener` instead of direct service calls
- No direct dependencies between user ↔ tenancy modules

### Foreign Key Constraint Violation

**Issue**: `update or delete on table "users" violates foreign key constraint`

**Solution**: The `UserEventListener` in the tenancy module automatically removes users from tenants before deletion. Verify:
- `UserEventListener` bean is created
- `UserDeletedEvent` is in `user.api` package
- Event is published before deletion

### Jackson LocalDateTime Error

**Issue**: `Java 8 date/time type LocalDateTime not supported`

**Solution**: The application includes `jackson-datatype-jsr310`. Verify:
- Dependency is in `pom.xml`
- `ObjectMapper` registers `JavaTimeModule`
- Maven dependencies are up to date

### Port Already in Use

**Issue**: `Port 2020 is already in use`

**Solution**:
```bash
# Find process using port
lsof -i :2020

# Kill process
kill -9 <PID>

# Or use different port
mvn spring-boot:run -Dserver.port=8080
```

## Production Deployment

### Build for Production

```bash
# Build optimized JAR
mvn clean package -DskipTests

# JAR location
target/archiving-0.0.1-SNAPSHOT.jar
```

### Docker Deployment

```bash
# Build image
docker build -t archiving-backend .

# Run container
docker run -p 2020:2020 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e DB_URL=jdbc:postgresql://db:5432/archiving \
  archiving-backend
```

### Environment Configuration

```bash
# Production profile
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:postgresql://prod-db:5432/archiving
export DB_USERNAME=prod_user
export DB_PASSWORD=secure_password
export SERVER_PORT=8080
```

### Health Checks

```bash
# Actuator endpoints (if enabled)
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
```

## API Documentation

### GraphQL Schema

View schema at: http://localhost:2020/graphiql

Or export schema:
```bash
curl http://localhost:2020/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ __schema { types { name } } }"}' \
  | jq .
```

### OpenAPI/Swagger

To add Swagger documentation:

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

Then access: http://localhost:2020/swagger-ui.html

## Performance Tips

1. **Enable caching**: Add `@EnableCaching` and cache frequently accessed data
2. **Use database indexes**: Index foreign keys and frequently queried columns
3. **Optimize queries**: Use `@EntityGraph` to prevent N+1 queries
4. **Connection pooling**: HikariCP is already configured
5. **Lazy loading**: Use `FetchType.LAZY` for collections

## Security Considerations

**⚠️ Current State: Development Mode**

For production, implement:

1. **Authentication**: Spring Security with JWT
2. **Authorization**: Role-based access control
3. **Password encryption**: Use BCrypt
4. **HTTPS**: Enable SSL/TLS
5. **CORS**: Configure allowed origins
6. **Rate limiting**: Prevent abuse
7. **Input validation**: Validate all inputs
8. **SQL injection**: Already prevented by JPA

## Useful Commands

```bash
# Clean build
mvn clean install

# Skip tests
mvn install -DskipTests

# Run specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=compose

# Check dependencies
mvn dependency:tree

# Update dependencies
mvn versions:display-dependency-updates

# Format code
mvn spotless:apply

# Generate documentation
mvn javadoc:javadoc
```

## Resources

- **Spring Boot**: https://spring.io/projects/spring-boot
- **Spring Modulith**: https://spring.io/projects/spring-modulith
- **GraphQL Java**: https://www.graphql-java.com/
- **Spring Data JPA**: https://spring.io/projects/spring-data-jpa
- **PostgreSQL**: https://www.postgresql.org/docs/

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review the generated documentation in `target/spring-modulith-docs/`
3. Check application logs in `logs/` directory
4. Review the GraphQL schema in `src/main/resources/graphql/schema.graphqls`

## License

[Add your license here]

---

**Last Updated**: February 1, 2026  
**Version**: 0.0.1-SNAPSHOT  
**Java Version**: 21  
**Spring Boot Version**: 3.5.4
