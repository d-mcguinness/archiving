# Archiving System

> A modern digital archiving platform supporting 9 international archiving standards with Spring Modulith backend and SvelteKit frontend.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![SvelteKit](https://img.shields.io/badge/SvelteKit-Latest-ff3e00.svg)](https://kit.svelte.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0-blue.svg)](https://www.typescriptlang.org/)

## Overview

A full-stack application for managing digital archives with support for multiple international archiving standards including NOARK5, OAIS, PREMIS, Dublin Core, METS, EAD, BagIt, ISAD(G), and MODS. Built with Spring Modulith for the backend and SvelteKit for the frontend.

## ✨ Features

- 🗄️ **Multi-Standard Archive Management** - Support for 9 international archiving standards
- 🏢 **Multi-Tenancy** - Organize archives by tenant organizations
- 👥 **User Management** - Complete user CRUD with role-based access
- 📊 **Hierarchical Elements** - Tree-structured archive organization
- 🔄 **GraphQL API** - Modern, efficient API with real-time updates
- 🎨 **Modern UI** - Responsive SvelteKit interface with toast notifications
- 🐳 **Docker Ready** - Easy deployment with Docker Compose
- 📦 **Modular Architecture** - Spring Modulith for maintainable backend
- 🔐 **Event-Driven** - Loose coupling via application events
- 📝 **Standard Compliance** - Export formats match archiving standard specifications

## 🚀 Quick Start

### Prerequisites

- **Backend**: Java 21, Maven 3.8+, PostgreSQL (or Docker)
- **Frontend**: Node.js 20+, npm/pnpm

### 1. Start Backend (Spring Boot)

```bash
# Using Docker Compose (recommended)
docker-compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=compose

# Or with embedded H2 database
mvn spring-boot:run

# Backend runs at http://localhost:2020
# GraphQL Playground: http://localhost:2020/graphiql
```

### 2. Start Frontend (SvelteKit)

```bash
cd frontend
npm install
npm run dev

# Frontend runs at http://localhost:3000
```

### 3. Access the Application

- **Frontend UI**: http://localhost:3000
- **GraphQL API**: http://localhost:2020/graphql
- **GraphQL Playground**: http://localhost:2020/graphiql

## 📚 Documentation

### Comprehensive Guides

- **[Backend Instructions](./INSTRUCTIONS.md)** - Spring Modulith backend setup, architecture, and API documentation
- **[Frontend Instructions](./frontend/INSTRUCTIONS.md)** - SvelteKit frontend setup, components, and features

### Quick Reference

#### Backend (Spring Boot + GraphQL)

```bash
# Start with Docker Compose
docker-compose up -d && mvn spring-boot:run -Dspring-boot.run.profiles=compose

# Run tests
mvn test

# Build JAR
mvn clean package

# Generate module documentation
mvn spring-modulith:docs
```

#### Frontend (SvelteKit)

```bash
# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Type check
npm run check
```

## 🏗️ Architecture

### Backend - Spring Modulith

```
┌─────────────────────────────────────────────────┐
│           Spring Modulith Backend               │
├─────────────────────────────────────────────────┤
│  User Module  │  Tenancy Module  │ Archive Module│
│  ┌─────────┐  │  ┌────────────┐  │ ┌───────────┐│
│  │  User   │  │  │   Tenant   │  │ │  Archive  ││
│  │ Service │  │  │  Service   │  │ │  Service  ││
│  └─────────┘  │  └────────────┘  │ └───────────┘│
│       ↓       │        ↓          │       ↓      │
│  ┌─────────┐  │  ┌────────────┐  │ ┌───────────┐│
│  │UserRepo │  │  │TenantRepo  │  │ │ArchiveRepo││
│  └─────────┘  │  └────────────┘  │ └───────────┘│
└──────┬────────┴─────────┬────────┴───────┬──────┘
       │                  │                │
       └──────── Events ──┴────────────────┘
                (Loose Coupling)
```

**Key Patterns:**
- **Modular Monolith** - Strict module boundaries with Spring Modulith
- **Event-Driven** - Modules communicate via application events
- **Strategy Pattern** - Archive operations vary by standard
- **GraphQL API** - Single endpoint, flexible queries

### Frontend - SvelteKit

```
┌─────────────────────────────────────────────────┐
│            SvelteKit Frontend                   │
├─────────────────────────────────────────────────┤
│  Routes         Components        Stores        │
│  /users    ──▶  UserCard    ──▶  toastStore    │
│  /tenants  ──▶  TenantCard  ──▶  Apollo Cache  │
│  /archives ──▶  ArchiveCard                     │
├─────────────────────────────────────────────────┤
│            Apollo Client (GraphQL)              │
└──────────────────┬──────────────────────────────┘
                   │
                   ↓
        GraphQL API (Backend)
```

**Key Features:**
- **File-Based Routing** - Automatic route generation
- **Apollo Client** - Efficient GraphQL data fetching
- **Toast Notifications** - User feedback system
- **Standard Schemas** - JSON definitions for 9 standards

## 🗄️ Supported Archiving Standards

| Standard | Description | Standard-Specific Features |
|----------|-------------|---------------------------|
| **NOARK5** | Norwegian electronic archives | `arkivdel`, `systemID`, `dokumentmedium` |
| **OAIS** | Digital preservation (ISO 14721) | SIP/AIP/DIP packages, PDI metadata |
| **PREMIS** | Preservation metadata | Object identifiers, preservation levels |
| **Dublin Core** | Simple metadata (ISO 15836) | 15 core elements (dc:title, dc:creator, etc.) |
| **METS** | Structural metadata | `metsHdr`, `dmdSec`, `amdSec`, `structMap` |
| **EAD** | Finding aids | `eadheader`, `archdesc`, `unittitle` |
| **BagIt** | File packaging (RFC 8493) | `bagit.txt`, `bag-info.txt`, manifests |
| **ISAD(G)** | International archival description | 26 elements in 7 areas |
| **MODS** | Bibliographic metadata | `titleInfo`, `originInfo`, `subject` |

Each standard has:
- ✅ Custom validation rules
- ✅ Standard-specific export format
- ✅ Metadata requirements documentation
- ✅ Frontend schema definition (JSON)

## 🔧 Technology Stack

### Backend

- **Framework**: Spring Boot 3.5.4
- **Architecture**: Spring Modulith (modular monolith)
- **Language**: Java 21
- **API**: GraphQL (via Spring GraphQL)
- **Database**: PostgreSQL (production), H2 (development)
- **ORM**: Spring Data JPA / Hibernate
- **Build**: Maven
- **Testing**: JUnit 5, Spring Modulith Tests

### Frontend

- **Framework**: SvelteKit
- **Language**: TypeScript
- **API Client**: Apollo Client
- **Build**: Vite
- **Styling**: Custom CSS with CSS variables
- **State**: Svelte Stores + Apollo Cache
- **Testing**: Vitest (unit), Playwright (e2e)

## 📋 Common Tasks

### Create a User

```bash
# Via GraphQL Playground (http://localhost:2020/graphiql)
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

# Or via Frontend UI
# Navigate to http://localhost:3000/users/create
```

### Create an Archive

```bash
# Via GraphQL
mutation {
  createArchive(input: {
    userId: 1
    title: "My Archive"
    description: "Digital archive for project documents"
    content: "{}"
    standard: NOARK5
  }) {
    id
    title
    standard
  }
}

# Or via Frontend UI
# Navigate to http://localhost:3000/archives/create
```

### Extract Archive (Download)

```bash
# Via REST API
curl -X POST http://localhost:2020/api/archives/1/extract \
  -H "Content-Type: application/json" \
  -d '{"password":"test"}' \
  -o archive_export.json

# Or via Frontend UI
# Click "Extract" button on archive list page
```

## 🐳 Docker Deployment

### Using Docker Compose

```bash
# Start PostgreSQL and application
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Build Docker Images

```bash
# Backend
docker build -t archiving-backend .

# Frontend
cd frontend
docker build -t archiving-frontend .
```

## 🧪 Testing

### Backend Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=ModulithStructureTest

# Generate module documentation
mvn spring-modulith:docs
# Output: target/spring-modulith-docs/
```

### Frontend Tests

```bash
cd frontend

# Unit tests
npm run test

# E2E tests
npm run test:e2e

# Type checking
npm run check
```

## 🔍 Project Structure

```
archiving/
├── src/                          # Backend source
│   ├── main/
│   │   ├── java/com/dmc/archiving/
│   │   │   ├── user/            # User module
│   │   │   ├── tenancy/         # Tenancy module
│   │   │   └── archive/         # Archive module
│   │   │       └── strategy/    # Strategy pattern implementations
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── data.sql
│   │       └── graphql/schema.graphqls
│   └── test/                    # Backend tests
├── frontend/                     # Frontend source
│   ├── src/
│   │   ├── routes/              # SvelteKit routes
│   │   ├── lib/                 # Shared libraries
│   │   │   ├── apollo.ts        # GraphQL client
│   │   │   ├── components/      # Reusable components
│   │   │   └── stores/          # State management
│   │   └── app.css              # Global styles
│   └── static/                  # Static assets
│       └── schemeDefinitions/   # Archive standard schemas
├── pom.xml                      # Maven configuration
├── compose.yaml                 # Docker Compose
├── Dockerfile                   # Backend Docker image
├── INSTRUCTIONS.md              # Backend documentation
└── README.md                    # This file
```

## 🔐 Security Considerations

**⚠️ Current Status: Development Mode**

For production deployment, implement:

- [ ] Authentication (Spring Security + JWT)
- [ ] Authorization (role-based access control)
- [ ] Password encryption (BCrypt)
- [ ] HTTPS/SSL
- [ ] CORS configuration
- [ ] Rate limiting
- [ ] Input validation
- [ ] API key management
- [ ] Audit logging

## 📊 Database Schema

### Main Tables

- `users` - User accounts
- `tenants` - Tenant organizations  
- `user_tenant` - Many-to-many user-tenant relationship
- `archives` - Archive records
- `elements` - Hierarchical archive elements (tree structure)
- `user_assignments` - User-archive role assignments

### Relationships

```
users ──┬── user_tenant ── tenants
        │
        └── user_assignments ── archives ── elements
```

## 🚨 Troubleshooting

### Backend won't start

```bash
# Check if port 2020 is in use
lsof -i :2020

# Check database connection
docker-compose logs db

# Verify Java version
java -version  # Should be 21+
```

### Frontend can't connect to backend

```bash
# Verify backend is running
curl http://localhost:2020/graphql

# Check CORS settings in backend
# Check VITE_GRAPHQL_URI in frontend/.env
```

### Circular dependency error

This has been fixed using event-driven architecture. If you encounter it:
- Check that `UserDeletedEvent` is in `user.api` package
- Verify `UserEventListener` exists in tenancy module
- Review the [circular dependency fix documentation](./CIRCULAR_DEPENDENCY_FIX.md)

## 📖 Additional Documentation

- [Backend Instructions](./INSTRUCTIONS.md) - Complete backend guide
- [Frontend Instructions](./frontend/INSTRUCTIONS.md) - Complete frontend guide
- [Archiving Standards Usage](./ARCHIVING_STANDARDS_USAGE.md) - Which countries and industries use each standard
- [Strategy Pattern Implementation](./STRATEGY_PATTERN_IMPLEMENTATION.md) - Archive standards
- [Toast Notification System](./TOAST_NOTIFICATION_SYSTEM.md) - User feedback
- [Circular Dependency Fix](./CIRCULAR_DEPENDENCY_FIX.md) - Event-driven architecture
- [Jackson DateTime Fix](./JACKSON_DATETIME_FIX.md) - Date serialization
- [Apollo Cache Fix](./APOLLO_CACHE_MERGE_FIX.md) - GraphQL cache management

## 🤝 Contributing

1. Follow Spring Modulith architecture principles
2. Add tests for new features
3. Update documentation
4. Follow existing code style
5. Use meaningful commit messages

## 📝 License

[Add your license here]

## 👨‍💻 Development Team

[Add your team information here]

## 🔗 Resources

### Backend
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Modulith](https://spring.io/projects/spring-modulith)
- [GraphQL Java](https://www.graphql-java.com/)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)

### Frontend
- [SvelteKit Documentation](https://kit.svelte.dev/)
- [Svelte Documentation](https://svelte.dev/)
- [Apollo Client](https://www.apollographql.com/docs/react/)
- [Vite](https://vitejs.dev/)

### Archiving Standards
- [NOARK5 Standard](https://www.arkivverket.no/en/about-us/regulations-and-standards/noark-5)
- [OAIS Reference Model (ISO 14721)](https://www.iso.org/standard/57284.html)
- [PREMIS Data Dictionary](https://www.loc.gov/standards/premis/)
- [Dublin Core](https://www.dublincore.org/)
- [METS](https://www.loc.gov/standards/mets/)
- [EAD](https://www.loc.gov/ead/)
- [BagIt (RFC 8493)](https://tools.ietf.org/html/rfc8493)
- [ISAD(G)](https://www.ica.org/en/isadg-general-international-standard-archival-description-second-edition)
- [MODS](https://www.loc.gov/standards/mods/)

---

**Version**: 0.0.1-SNAPSHOT  
**Last Updated**: February 1, 2026  
**Status**: Development  

For detailed setup and usage instructions, see the [Backend Instructions](./INSTRUCTIONS.md) and [Frontend Instructions](./frontend/INSTRUCTIONS.md).
