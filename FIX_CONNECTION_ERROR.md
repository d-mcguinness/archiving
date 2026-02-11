# SOLUTION: Failed to Fetch Error

## Error Message
```
Load archives error: ApolloError: Failed to fetch
POST http://localhost:2020/graphql net::ERR_CONNECTION_REFUSED
```

## Root Cause
**The Spring Boot backend is not running.**

The frontend (running on port 3000) is trying to connect to the backend (should be on port 2020), but nothing is listening on that port.

---

## SOLUTION: Start the Backend

### Quick Fix - Run These Commands:

Open a **new terminal** and run:

```bash
# Navigate to project
cd /Users/dmcg/workspace2/archiving

# Start database
docker compose up -d db

# Wait for database to be ready (important!)
sleep 10

# Start Spring Boot (this will run in foreground)
mvn spring-boot:run
```

**Keep this terminal open!** The Spring Boot application will run here.

Wait until you see:
```
Started ArchivingApplication in X.XXX seconds
```

Then refresh your browser at http://localhost:3000

---

## Alternative: Use the Startup Script

I created a startup script for you:

```bash
cd /Users/dmcg/workspace2/archiving
./start-backend.sh
```

This will:
1. Start the database
2. Wait for it to be ready
3. Start Spring Boot

---

## Full System Startup

You need **2 terminals**:

### Terminal 1: Backend
```bash
cd /Users/dmcg/workspace2/archiving
./start-backend.sh
```

### Terminal 2: Frontend
```bash
cd /Users/dmcg/workspace2/archiving/frontend  
npm run dev
```

---

## Verification

### 1. Check Backend is Running

Test with curl:
```bash
curl http://localhost:2020/graphiql
```

Should return HTML (GraphiQL interface)

Or open in browser: http://localhost:2020/graphiql

### 2. Check GraphQL Works

```bash
curl http://localhost:2020/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"{ __typename }"}'
```

Should return:
```json
{"data":{"__typename":"Query"}}
```

### 3. Check Frontend Loads Archives

Open browser: http://localhost:3000/archives

You should see your archives list (or empty state if no archives yet)

---

## Common Issues

### Docker not installed/running

If `docker compose` fails:
- Install Docker Desktop
- Start Docker Desktop
- Try again

### Port 2020 already in use

```bash
# Find what's using it
lsof -i :2020

# Kill it (replace PID)
kill -9 <PID>
```

### Database connection errors

```bash
# Check database logs
docker compose logs db

# Restart database
docker compose down
docker compose up -d db
```

### Maven not installed

```bash
# Install Maven with Homebrew
brew install maven

# Verify
mvn --version
```

---

## Development Workflow

### Starting Work
1. Open Terminal 1: `./start-backend.sh`
2. Open Terminal 2: `cd frontend && npm run dev`
3. Open browser: http://localhost:3000

### During Development
- Backend changes: Stop (Ctrl+C) and restart `mvn spring-boot:run`
- Frontend changes: Stop (Ctrl+C) and restart `npm run dev`
- Database reset: `docker compose down && docker compose up -d db`

### Ending Work
1. Terminal 1: Press Ctrl+C (stops backend)
2. Terminal 2: Press Ctrl+C (stops frontend)
3. (Optional) `docker compose down` (stops database)

---

## System Requirements

✅ Java 17 or higher  
✅ Maven 3.6+  
✅ Node.js 18+  
✅ Docker Desktop  
✅ PostgreSQL (via Docker)  

---

## Quick Reference

| What           | Command                          | Port |
|----------------|----------------------------------|------|
| Start DB       | `docker compose up -d db`        | 5432 |
| Start Backend  | `mvn spring-boot:run`            | 2020 |
| Start Frontend | `cd frontend && npm run dev`     | 3000 |
| GraphiQL       | Open http://localhost:2020/graphiql | - |
| App            | Open http://localhost:3000       | -    |

---

## NEXT STEPS

1. **Start the backend** using one of the methods above
2. **Wait for it to fully start** (look for "Started ArchivingApplication")
3. **Refresh your browser** at http://localhost:3000
4. **The error will be gone** and archives will load!

The extract feature and all other functionality will work once the backend is running.
