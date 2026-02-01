# How to Start the Archive System

## Error: Failed to fetch / Connection Refused

If you see this error:
```
POST http://localhost:2020/graphql net::ERR_CONNECTION_REFUSED
```

This means the **Spring Boot backend is not running**.

---

## Quick Start Guide

### Option 1: Using the Startup Script (Recommended)

```bash
cd /Users/dmcg/workspace2/archiving
./start-backend.sh
```

This script will:
1. Start the PostgreSQL database
2. Wait for it to be ready
3. Start the Spring Boot application

### Option 2: Manual Startup

#### Step 1: Start PostgreSQL Database

```bash
cd /Users/dmcg/workspace2/archiving
docker compose up -d db
```

Wait a few seconds for the database to start, then verify:
```bash
docker compose ps
```

You should see:
```
NAME                     STATUS
archiving-db-1          Up (healthy)
```

#### Step 2: Start Spring Boot Backend

```bash
cd /Users/dmcg/workspace2/archiving
mvn spring-boot:run
```

Wait for the application to start. You should see:
```
Started ArchivingApplication in X.XXX seconds
```

#### Step 3: Start Frontend (in a new terminal)

```bash
cd /Users/dmcg/workspace2/archiving/frontend
npm run dev
```

---

## Verification

### Check Backend is Running

Test the GraphQL endpoint:
```bash
curl http://localhost:2020/graphiql
```

Or visit in browser: http://localhost:2020/graphiql

### Check Frontend is Running

Visit in browser: http://localhost:3000

---

## Troubleshooting

### Issue: Database won't start

**Check Docker is running:**
```bash
docker ps
```

**Check logs:**
```bash
docker compose logs db
```

**Reset database:**
```bash
docker compose down
docker compose up -d db
```

### Issue: Spring Boot fails to start

**Check if port 2020 is already in use:**
```bash
lsof -i :2020
```

If something is using the port, kill it:
```bash
kill -9 <PID>
```

**Check database connection:**
```bash
docker compose logs db
```

Make sure the database is running and healthy.

**Check application logs:**
Look for error messages in the terminal where you ran `mvn spring-boot:run`

**Common errors:**
- Database not ready: Wait longer or check database status
- Port in use: Kill the process using port 2020
- Compilation errors: Run `mvn clean compile` first

### Issue: Frontend won't connect

**Check backend is running:**
```bash
curl http://localhost:2020/graphql -H "Content-Type: application/json" -d '{"query":"{ __typename }"}'
```

Should return:
```json
{"data":{"__typename":"Query"}}
```

**Check proxy configuration:**
Frontend should proxy `/graphql` and `/api` to `localhost:2020`

File: `/frontend/vite.config.js`
```javascript
proxy: {
    '/graphql': {
        target: 'http://localhost:2020',
        changeOrigin: true
    },
    '/api': {
        target: 'http://localhost:2020',
        changeOrigin: true
    }
}
```

**Restart frontend after proxy changes:**
```bash
# Stop frontend (Ctrl+C)
cd frontend
npm run dev
```

---

## Complete System Architecture

```
┌─────────────────────────────────────────────┐
│  Frontend (Vite + SvelteKit)                │
│  http://localhost:3000                      │
│  - Serves UI                                │
│  - Proxies /graphql → :2020                 │
│  - Proxies /api → :2020                     │
└──────────────────┬──────────────────────────┘
                   │
                   ↓
┌─────────────────────────────────────────────┐
│  Spring Boot Backend                        │
│  http://localhost:2020                      │
│  - GraphQL API at /graphql                  │
│  - REST API at /api/*                       │
│  - Extract endpoint: /api/archives/{id}/extract │
└──────────────────┬──────────────────────────┘
                   │
                   ↓
┌─────────────────────────────────────────────┐
│  PostgreSQL Database                        │
│  localhost:5432                             │
│  Database: archiving                        │
│  User: archiving_user                       │
└─────────────────────────────────────────────┘
```

---

## Development Workflow

### Start Everything

**Terminal 1 - Database & Backend:**
```bash
cd /Users/dmcg/workspace2/archiving
./start-backend.sh
```

**Terminal 2 - Frontend:**
```bash
cd /Users/dmcg/workspace2/archiving/frontend
npm run dev
```

### Stop Everything

**Stop Frontend:** Press `Ctrl+C` in Terminal 2

**Stop Backend:** Press `Ctrl+C` in Terminal 1

**Stop Database:**
```bash
docker compose down
```

### Restart Backend Only

**Stop:** Press `Ctrl+C` in Terminal 1

**Start:**
```bash
mvn spring-boot:run
```

### Restart Frontend Only

**Stop:** Press `Ctrl+C` in Terminal 2

**Start:**
```bash
cd frontend
npm run dev
```

---

## Port Reference

| Service    | Port | URL                              |
|------------|------|----------------------------------|
| Frontend   | 3000 | http://localhost:3000           |
| Backend    | 2020 | http://localhost:2020           |
| GraphiQL   | 2020 | http://localhost:2020/graphiql  |
| Database   | 5432 | localhost:5432                  |

---

## Quick Health Check

Run this command to check all services:

```bash
echo "Checking services..."
echo "Frontend: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:3000)"
echo "Backend GraphQL: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:2020/graphiql)"
echo "Database: $(docker compose ps db | grep -q Up && echo 'Running' || echo 'Not running')"
```

Expected output:
```
Checking services...
Frontend: 200
Backend GraphQL: 200
Database: Running
```

---

## Summary

✅ **Start backend:** `./start-backend.sh`  
✅ **Start frontend:** `cd frontend && npm run dev`  
✅ **Access app:** http://localhost:3000  
✅ **GraphiQL:** http://localhost:2020/graphiql  

The "Failed to fetch" error occurs when the backend is not running. Just start it using the script above!
