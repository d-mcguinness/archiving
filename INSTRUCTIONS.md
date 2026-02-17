# Archiving System - Backend Instructions

## Overview

This is a **Spring Modulith** application for managing digital archives with support for multiple international archiving standards (NOARK5, OAIS, PREMIS, Dublin Core, METS, EAD, BagIt, ISAD(G), MODS).

The application implements a **role-based access control (RBAC)** system with three distinct roles:
- **ADMIN**: Full system access - manage tenants, users, and archives
- **TENANT**: Organization management - manage users and archives within their tenant
- **USER**: Document submission - submit and view their own documents

## Tech Stack

- **Framework**: Spring Boot 3.5.4
- **Java**: 21
- **Architecture**: Spring Modulith (modular monolith)
- **API**: GraphQL + REST
- **Database**: PostgreSQL (production), H2 (development)
- **ORM**: JPA/Hibernate
- **Build Tool**: Maven
- **Frontend**: SvelteKit (separate application)
- **Data Model**: Entity-Attribute-Value (EAV) pattern - see [ELEMENT_FIELD_ARCHITECTURE_ANALYSIS.md](ELEMENT_FIELD_ARCHITECTURE_ANALYSIS.md)

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

## Role-Based Access Control (RBAC)

### User Roles

The system implements three distinct user roles with different access levels:

#### ADMIN Role
**Full system access** - Complete control over all resources

**Permissions**:
- ✅ Manage all tenants (create, read, update, delete)
- ✅ Manage all users across all tenants
- ✅ Manage all archives across all tenants
- ✅ Access admin panel
- ✅ View system-wide statistics and analytics
- ✅ Configure system settings

**Use Cases**:
- System administrators
- IT staff
- Super users

#### TENANT Role
**Organization management** - Manage resources within their tenant

**Permissions**:
- ✅ Manage users within their tenant
- ✅ Manage archives within their tenant
- ✅ View tenant-specific statistics
- ❌ Cannot manage other tenants
- ❌ Cannot access admin panel
- ❌ Cannot view system-wide data

**Use Cases**:
- Organization managers
- Department heads
- Team leaders

#### USER Role
**Document submission** - Basic access for document submission

**Permissions**:
- ✅ Submit documents for archiving
- ✅ View their own submitted documents
- ❌ Cannot manage other users
- ❌ Cannot manage archives
- ❌ Cannot access admin features

**Use Cases**:
- Regular employees
- Document submitters
- End users

### Role-Based Navigation

The frontend adjusts navigation based on user role:

**ADMIN Navigation**:
```
🏛️ Archiving System | [Tenants] [Users] [Archives] | 👤 Admin | [Logout]
```

**TENANT Navigation**:
```
🏛️ Archiving System | [Users] [Archives] | 👤 Manager | [Logout]
```

**USER Navigation**:
```
🏛️ Archiving System | 👤 User | [Logout]
```

### Role-Based Dashboard Views

#### ADMIN Dashboard
Shows comprehensive system statistics:
- Total users across all tenants
- Total tenants in the system
- Total archives (all statuses)
- Archive status breakdown (Published, Draft, Archived)
- Archive distribution by standard
- Quick action cards for all management tasks
- Recent archives from all tenants

#### TENANT Dashboard
Shows organization-specific data:
- Users in their tenant
- Archives in their tenant
- Archive status breakdown for their archives
- Quick action cards for user and archive management
- Recent archives from their tenant

#### USER Dashboard
Shows document submission interface:
- Welcome message with user name
- Simple document upload form
- Information about the archiving process
- No statistics or management features
- Focused on document submission workflow

### Authentication Flow

```
1. User navigates to /login
2. Selects role (Admin, Tenant, or User)
3. Clicks "Sign In" (demo credentials auto-filled)
4. System stores:
   - auth_token (JWT token)
   - auth_user (user object)
   - auth_role (ADMIN/TENANT/USER)
5. Redirects to role-appropriate dashboard
6. Navigation and features adjust based on role
```

### Demo Credentials

For testing purposes, the application provides demo login cards:

```
👑 Admin
Username: admin
Password: admin123
Role: ADMIN

🏢 Tenant
Username: tenant
Password: tenant123
Role: TENANT

👤 User
Username: user
Password: user123
Role: USER
```

### Implementing Role Checks

#### Backend (Spring Security - Future Implementation)

```java
@PreAuthorize("hasRole('ADMIN')")
@QueryMapping
public List<Tenant> getAllTenants() {
    return tenancyService.getAllTenants();
}

@PreAuthorize("hasAnyRole('ADMIN', 'TENANT')")
@QueryMapping
public List<User> getUsers() {
    return userService.getAllUsers();
}

@PreAuthorize("hasAnyRole('ADMIN', 'TENANT', 'USER')")
@PostMapping("/api/upload")
public ResponseEntity<?> uploadDocument(@RequestParam MultipartFile file) {
    return fileUploadService.upload(file);
}
```

#### Frontend (Current Implementation)

```typescript
// Check role from localStorage
const role = localStorage.getItem('auth_role');

// Conditionally render navigation
{#if role === 'ADMIN' || role === 'TENANT'}
  <a href="/archives">Archives</a>
{/if}

{#if role === 'ADMIN'}
  <a href="/tenants">Tenants</a>
{/if}
```

### Security Considerations

**⚠️ Current State**: Client-side role checking only (development mode)

**For Production**:
1. Implement Spring Security with JWT authentication
2. Add `@PreAuthorize` annotations to all endpoints
3. Validate roles on the backend for every request
4. Use secure session management
5. Implement password encryption (BCrypt)
6. Add HTTPS/TLS encryption
7. Implement rate limiting
8. Add audit logging for sensitive operations

**Important**: Never rely solely on client-side role checks for security!

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

## Element & Field Architecture (EAV Pattern)

### Overview

The system uses an **Entity-Attribute-Value (EAV)** pattern to support all 9 archiving standards without database schema changes. This flexible approach is essential for handling diverse hierarchical structures across standards.

### Core Entities

```java
Archive (1) ─────→ (n) Element ─────→ (n) Field
                      └── (n) Element (recursive hierarchy)
```

**Why EAV?**
- ✅ Supports 9+ standards (NOARK5, OAIS, PREMIS, Dublin Core, METS, EAD, BagIt, ISAD(G), MODS)
- ✅ Add new standards without database migrations
- ✅ Handle arbitrary hierarchy depths
- ✅ Flexible metadata per standard
- ✅ Future-proof for standard evolution

### Element Entity

Represents structural units in an archive (e.g., NOARK5 "Series", Dublin Core "Resource", OAIS "SIP"):

```java
@Entity
public class Element {
    private Long id;
    private Archive archive;
    private Element parent;              // Recursive hierarchy
    private List<Element> children;      // Child elements
    private List<Field> fields;          // Metadata fields
    private String entityType;           // e.g., "Series", "Resource"
    private String title;
    private Boolean isRoot;
    // ... lifecycle fields
}
```

**Key Methods:**
- `getPath()` - Full hierarchical path (e.g., "/arkiv/arkivdel/mappe")
- `getDepth()` - Depth in hierarchy (root = 0)
- `isLeaf()` - Check if element has children
- `countDescendants()` - Total descendants

### Field Entity

Represents metadata values for Elements:

```java
@Entity
public class Field {
    private Long id;
    private Element element;
    private String name;      // e.g., "systemID", "dc:title"
    private String label;     // Human-readable label
    private String type;      // "string", "date", "integer"
    private String value;     // TEXT storage
}
```

### Example: NOARK5 Structure

```java
// Create NOARK5 archive hierarchy
Archive archive = new Archive(standard = ArchiveStandard.NOARK5);

Element arkiv = new Element(entityType = "Archive", isRoot = true);
arkiv.addField(new Field(name = "systemID", value = "ARK-001"));
arkiv.addField(new Field(name = "title", value = "Corporate Archive"));

Element arkivdel = new Element(entityType = "Series", parent = arkiv);
arkivdel.addField(new Field(name = "systemID", value = "2024"));
arkivdel.addField(new Field(name = "title", value = "Financial Records"));

Element mappe = new Element(entityType = "File", parent = arkivdel);
mappe.addField(new Field(name = "fileID", value = "2024/001"));
```

### Performance Optimization

**Use @EntityGraph for loading:**
```java
@EntityGraph(attributePaths = {"elements", "elements.fields"})
Archive findByIdWithElements(Long id);
```

**Cache archives:**
```java
@Cacheable(value = "archives", key = "#id")
public Archive findByIdWithElements(Long id);
```

**Add indexes:**
```sql
CREATE INDEX idx_element_archive_id ON elements(archive_id);
CREATE INDEX idx_field_element_id ON fields(element_id);
CREATE INDEX idx_element_entity_type ON elements(entity_type);
```

### Best Practices

✅ **DO:**
- Validate Element hierarchy using Strategy pattern
- Use `@EntityGraph` when loading archives
- Cache frequently accessed archives
- Validate Field types before saving

❌ **DON'T:**
- Load lazy Fields without JOIN FETCH
- Skip validation - EAV is too flexible
- Query Fields directly - go through Elements
- Mix standards in one Archive

### Detailed Analysis

For comprehensive analysis, performance benchmarks, and alternative approaches, see:
**[ELEMENT_FIELD_ARCHITECTURE_ANALYSIS.md](ELEMENT_FIELD_ARCHITECTURE_ANALYSIS.md)**

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

**⚠️ Current State: Development Mode with Client-Side RBAC**

The application currently implements **client-side role-based access control** for development and demonstration purposes. The frontend checks user roles from `localStorage` to show/hide features.

### Current Implementation

**Frontend Role Checking**:
```typescript
// Stored in localStorage after login
const role = localStorage.getItem('auth_role'); // 'ADMIN' | 'TENANT' | 'USER'
const user = localStorage.getItem('auth_user');
const token = localStorage.getItem('auth_token');

// Conditional rendering based on role
{#if role === 'ADMIN'}
  <a href="/tenants">Manage Tenants</a>
{/if}
```

**Access Control by Route**:
- `/admin` - Redirects non-ADMIN users
- `/tenants` - Only visible to ADMIN
- `/users` - Visible to ADMIN and TENANT
- `/archives` - Visible to ADMIN and TENANT
- Dashboard (/) - Customized view for each role

### Production Requirements

For production deployment, implement the following security measures:

#### 1. **Backend Authentication & Authorization**

```java
// Spring Security Configuration
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/tenants/**").hasAnyRole("ADMIN", "TENANT")
                .requestMatchers("/api/archives/**").hasAnyRole("ADMIN", "TENANT")
                .requestMatchers("/api/upload/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
            .build();
    }
}

// Method-level security
@PreAuthorize("hasRole('ADMIN')")
@QueryMapping
public List<Tenant> getAllTenants() {
    return tenancyService.getAllTenants();
}

@PreAuthorize("hasAnyRole('ADMIN', 'TENANT')")
@MutationMapping
public User createUser(CreateUserInput input) {
    return userService.createUser(input);
}
```

#### 2. **JWT Token-Based Authentication**

```java
// Token generation on login
public String generateToken(User user) {
    return Jwts.builder()
        .setSubject(user.getEmail())
        .claim("role", user.getRole())
        .claim("tenantId", user.getTenantId())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24 hours
        .signWith(SignatureAlgorithm.HS512, secretKey)
        .compact();
}

// Token validation
public Claims validateToken(String token) {
    return Jwts.parser()
        .setSigningKey(secretKey)
        .parseClaimsJws(token)
        .getBody();
}
```

#### 3. **Password Encryption**

```java
@Configuration
public class PasswordConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

// Usage
String hashedPassword = passwordEncoder.encode(plainPassword);
boolean matches = passwordEncoder.matches(plainPassword, hashedPassword);
```

#### 4. **HTTPS/TLS**

```properties
# application.properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_PASSWORD}
server.ssl.key-store-type=PKCS12
```

#### 5. **CORS Configuration**

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList("https://yourdomain.com"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

#### 6. **Rate Limiting**

```java
@Configuration
public class RateLimitConfig {
    
    @Bean
    public RateLimiter rateLimiter() {
        return RateLimiter.create(100.0); // 100 requests per second
    }
}
```

#### 7. **Input Validation**

```java
@Data
@Validated
public class CreateUserInput {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    @Min(value = 18, message = "User must be at least 18 years old")
    private Integer age;
}
```

#### 8. **Audit Logging**

```java
@Aspect
@Component
public class AuditAspect {
    
    @Around("@annotation(PreAuthorize)")
    public Object logSecureAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String user = SecurityContextHolder.getContext().getAuthentication().getName();
        String method = joinPoint.getSignature().getName();
        
        log.info("User {} accessed secure method: {}", user, method);
        
        Object result = joinPoint.proceed();
        
        log.info("User {} completed method: {}", user, method);
        return result;
    }
}
```

### Session Management

```java
@Configuration
public class SessionConfig {
    
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
    
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
```

### Security Headers

```java
http.headers(headers -> headers
    .contentSecurityPolicy("default-src 'self'")
    .frameOptions().deny()
    .xssProtection().block(true)
    .contentTypeOptions()
);
```

### Additional Security Measures

1. **SQL Injection**: ✅ Already prevented by JPA/Hibernate parameterized queries
2. **XSS Protection**: Add input sanitization and CSP headers
3. **CSRF Protection**: Enable Spring Security CSRF tokens
4. **Clickjacking**: Use `X-Frame-Options: DENY` header
5. **Secrets Management**: Use environment variables or secrets manager
6. **Dependency Scanning**: Regularly update dependencies and scan for CVEs
7. **Security Testing**: Implement penetration testing and security audits

### Role-Based Access Matrix

| Feature | ADMIN | TENANT | USER |
|---------|-------|--------|------|
| View Dashboard | ✅ | ✅ | ✅ |
| Manage Tenants | ✅ | ❌ | ❌ |
| Manage Users | ✅ | ✅ (own tenant) | ❌ |
| Manage Archives | ✅ | ✅ (own tenant) | ❌ |
| Submit Documents | ✅ | ✅ | ✅ |
| View All Statistics | ✅ | ❌ | ❌ |
| Access Admin Panel | ✅ | ❌ | ❌ |
| Export Archives | ✅ | ✅ (own tenant) | ❌ |
| Delete Resources | ✅ | ✅ (own tenant) | ❌ |

### Testing Security

```bash
# Test ADMIN access
curl -X POST http://localhost:2020/graphql \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"query": "{ getAllTenants { id name } }"}'

# Test TENANT access (should fail for tenants)
curl -X POST http://localhost:2020/graphql \
  -H "Authorization: Bearer ${TENANT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"query": "{ getAllTenants { id name } }"}'

# Test unauthorized access (should fail)
curl -X POST http://localhost:2020/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ getAllUsers { id name } }"}'
```

### Migration Checklist

When moving from development to production:

- [ ] Implement Spring Security
- [ ] Add JWT authentication
- [ ] Enable password encryption
- [ ] Configure HTTPS/TLS
- [ ] Set up proper CORS
- [ ] Add rate limiting
- [ ] Implement audit logging
- [ ] Add input validation
- [ ] Enable security headers
- [ ] Set up secrets management
- [ ] Configure session management
- [ ] Add security testing
- [ ] Review and update all endpoints
- [ ] Remove demo credentials
- [ ] Add proper user registration
- [ ] Implement password reset flow

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

**Last Updated**: February 12, 2026  
**Version**: 0.0.1-SNAPSHOT  
**Java Version**: 21  
**Spring Boot Version**: 3.5.4  
**Security**: Client-side RBAC (Development) - Backend authentication required for production
