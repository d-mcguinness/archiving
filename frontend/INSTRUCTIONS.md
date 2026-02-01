# Archiving System - Frontend Instructions

## Overview

This is a **SvelteKit** application that provides a modern web interface for managing digital archives. It connects to the Spring Boot GraphQL backend and supports multiple international archiving standards.

## Tech Stack

- **Framework**: SvelteKit
- **Language**: TypeScript
- **API Client**: Apollo Client (GraphQL)
- **Styling**: Custom CSS
- **Build Tool**: Vite
- **Package Manager**: npm/pnpm

## Prerequisites

- **Node.js**: 18+ or 20+
- **npm** or **pnpm**
- **Backend API**: Running on http://localhost:2020

## Project Structure

```
frontend/
├── src/
│   ├── routes/                  # SvelteKit routes
│   │   ├── +layout.svelte       # Root layout
│   │   ├── +page.svelte         # Home page
│   │   ├── archives/            # Archive management
│   │   │   ├── +page.svelte     # List archives
│   │   │   ├── create/          # Create archive
│   │   │   ├── update/[id]/     # Update archive
│   │   │   ├── delete/[id]/     # Delete archive
│   │   │   └── document/        # Upload document
│   │   ├── users/               # User management
│   │   │   ├── +page.svelte     # List users
│   │   │   ├── create/          # Create user
│   │   │   ├── update/          # Update user
│   │   │   └── delete/          # Delete user
│   │   └── tenants/             # Tenant management
│   │       ├── +page.svelte     # List tenants
│   │       ├── create/          # Create tenant
│   │       ├── update/          # Update tenant
│   │       └── delete/          # Delete tenant
│   ├── lib/
│   │   ├── apollo.ts            # Apollo Client config
│   │   ├── components/          # Reusable components
│   │   │   └── Toast.svelte     # Toast notifications
│   │   ├── stores/              # Svelte stores
│   │   │   └── toastStore.ts    # Toast state management
│   │   └── graphql/
│   │       └── queries.ts       # GraphQL queries/mutations
│   ├── app.css                  # Global styles
│   └── app.html                 # HTML template
├── static/
│   ├── favicon.png
│   └── schemeDefinitions/       # Archive standard schemas
│       ├── noark5.json
│       ├── oais.json
│       ├── premis.json
│       ├── dublin-core.json
│       ├── mets.json
│       ├── ead.json
│       ├── bagit.json
│       ├── isadg.json
│       └── mods.json
├── package.json
├── svelte.config.js
├── tsconfig.json
└── vite.config.js
```

## Quick Start

### 1. Install Dependencies

```bash
cd frontend
npm install
# or
pnpm install
```

### 2. Start Development Server

```bash
npm run dev
# or
pnpm dev
```

The application will be available at:
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:2020/graphql (must be running)

### 3. Build for Production

```bash
npm run build
npm run preview
```

## Configuration

### Environment Variables

Create a `.env` file in the `frontend/` directory:

```env
# Backend GraphQL endpoint
VITE_GRAPHQL_URI=http://localhost:2020/graphql

# Other settings
VITE_API_TIMEOUT=30000
```

### Apollo Client Configuration

Located in `src/lib/apollo.ts`:

```typescript
const uri = import.meta.env?.VITE_GRAPHQL_URI || '/graphql';

const cache = new InMemoryCache({
  typePolicies: {
    Query: {
      fields: {
        getAllUsers: { merge: (_, incoming) => incoming },
        getAllArchives: { merge: (_, incoming) => incoming },
        getAllTenants: { merge: (_, incoming) => incoming },
      }
    }
  }
});
```

**Key Features:**
- Automatic error handling
- Cache management with custom merge functions
- Network error detection
- CORS support

## Features

### 1. User Management

**List Users** (`/users`)
- View all users in a table
- Search and filter
- Quick actions: Edit, Delete

**Create User** (`/users/create`)
- Name, email, age fields
- Real-time validation
- Success/error toast notifications

**Update User** (`/users/update`)
- Pre-filled form with current data
- Update name, email, age
- Toast notification on success/failure

**Delete User** (`/users/delete`)
- Confirmation dialog
- Automatic cleanup of tenant associations (via backend)
- Toast notification

### 2. Tenant Management

**List Tenants** (`/tenants`)
- View all tenants with status badges
- Filter by status (Active, Inactive, etc.)
- Tenant cards with details

**Create Tenant** (`/tenants/create`)
- Name, domain, display name, description
- Owner selection (from users)
- Plan selection (FREE, BASIC, PROFESSIONAL, etc.)
- Toast notification

**Update Tenant** (`/tenants/update`)
- Edit tenant details
- Change plan and status
- Toast notification

**Delete Tenant** (`/tenants/delete`)
- Confirmation dialog
- Toast notification

### 3. Archive Management

**List Archives** (`/archives`)
- View all archives
- Standard badges (NOARK5, OAIS, etc.)
- Status indicators
- Quick actions: Edit, Delete, Upload Document, Extract

**Create Archive** (`/archives/create`)
- Multi-step wizard
- Standard selection (9 supported standards)
- Dynamic form based on selected standard
- Visual canvas for hierarchical elements
- Toast notification

**Update Archive** (`/archives/update/[id]`)
- Edit archive metadata
- Update status
- Manage elements
- Toast notification

**Upload Document** (`/archives/document`)
- Standard-specific form fields
- File upload
- Metadata extraction
- Toast notification

**Extract Archive** (Download)
- Password protection
- Standard-specific export format
- Downloads as JSON file
- Toast notification

**Delete Archive** (`/archives/delete/[id]`)
- Confirmation dialog
- Cascading deletion of elements
- Toast notification

### 4. Archive Standards

The application supports 9 international archiving standards:

| Standard | Description | Use Case |
|----------|-------------|----------|
| **NOARK5** | Norwegian electronic archives | Government/public sector archives (Norway) |
| **OAIS** | Open Archival Information System | Digital preservation (ISO 14721) |
| **PREMIS** | Preservation Metadata | Digital object preservation |
| **Dublin Core** | Simple metadata (15 elements) | General-purpose metadata |
| **METS** | Metadata Encoding & Transmission | Structural metadata & packaging |
| **EAD** | Encoded Archival Description | Finding aids & archival descriptions |
| **BagIt** | File packaging (RFC 8493) | Reliable file transport |
| **ISAD(G)** | International archival description | Multilevel archival descriptions |
| **MODS** | Metadata Object Description | Bibliographic metadata |

Each standard has a JSON schema definition in `static/schemeDefinitions/`.

## Toast Notifications

The application uses a custom toast notification system for user feedback.

### Usage

```typescript
import { toasts } from '$lib/stores/toastStore';

// Success
toasts.add('User created successfully', 'success');

// Error
toasts.add('Failed to delete archive', 'error');

// Warning
toasts.add('Archive needs validation', 'warning');

// Info
toasts.add('Processing...', 'info');

// Custom duration (default: 5000ms)
toasts.add('Message', 'success', 3000);
```

### Toast Types

- **Success** (green): Operation completed successfully
- **Error** (red): Operation failed
- **Warning** (orange): Cautionary message
- **Info** (blue): Informational message

### Features

- Auto-dismiss after 5 seconds
- Manual close button
- Stacks multiple toasts
- Smooth animations
- Mobile responsive

## GraphQL Queries & Mutations

### Common Queries

```graphql
# Get all users
query GetAllUsers {
  getAllUsers {
    id
    name
    email
    age
  }
}

# Get specific archive
query GetArchive($id: ID!) {
  getArchive(id: $id) {
    id
    title
    standard
    status
    createdAt
    rootElement {
      id
      title
    }
  }
}
```

### Common Mutations

```graphql
# Create archive
mutation CreateArchive($input: CreateArchiveInput!) {
  createArchive(input: $input) {
    id
    title
    standard
  }
}

# Delete user
mutation DeleteUser($id: ID!) {
  deleteUser(id: $id)
}
```

All queries are defined in `src/lib/graphql/queries.ts`.

## Routing

SvelteKit uses file-based routing:

```
routes/
├── +page.svelte              → /
├── users/
│   ├── +page.svelte          → /users
│   ├── create/+page.svelte   → /users/create
│   ├── update/+page.svelte   → /users/update?userId=1
│   └── delete/+page.svelte   → /users/delete?userId=1
└── archives/
    ├── update/
    │   └── [id]/+page.svelte → /archives/update/1
    └── delete/
        └── [id]/+page.svelte → /archives/delete/1
```

### Navigation

```typescript
import { goto } from '$app/navigation';

// Navigate to list
goto('/users');

// Navigate with query params
goto('/users/update?userId=1');

// Navigate to dynamic route
goto(`/archives/update/${archiveId}`);
```

## Styling

### Global Styles

Located in `src/app.css`:
- CSS custom properties (variables)
- Reset styles
- Typography
- Layout utilities

### Component Styles

Each Svelte component has scoped `<style>` blocks:

```svelte
<style>
  .button {
    background: var(--primary-color);
    color: white;
    /* Styles are scoped to this component */
  }
</style>
```

### CSS Variables

```css
:root {
  --primary-color: #3b82f6;
  --success-color: #10b981;
  --error-color: #ef4444;
  --warning-color: #f59e0b;
  /* etc. */
}
```

## State Management

### Svelte Stores

```typescript
// Create store
import { writable } from 'svelte/store';
export const myStore = writable(initialValue);

// Use in component
import { myStore } from '$lib/stores/myStore';
$myStore // Auto-subscribes
```

### Apollo Client Cache

Apollo Client manages GraphQL query cache automatically:

```typescript
// Refetch queries after mutation
await client.mutate({
  mutation: DELETE_USER,
  variables: { id: userId },
  refetchQueries: [{ query: GET_ALL_USERS }],
  awaitRefetchQueries: true
});
```

## Development

### Hot Module Replacement (HMR)

Vite provides instant HMR. Changes to `.svelte` files reload instantly without losing state.

### TypeScript

The project uses TypeScript for type safety:

```typescript
// Define types
interface User {
  id: string;
  name: string;
  email: string;
  age?: number;
}

// Use types
let users: User[] = [];
```

### Linting & Formatting

```bash
# Run linter
npm run lint

# Format code
npm run format

# Type check
npm run check
```

## Building for Production

### Build

```bash
npm run build
```

Output: `.svelte-kit/output/`

### Preview

```bash
npm run preview
```

Serves the production build at http://localhost:4173

### Static Adapter (Optional)

To generate a static site:

1. Install adapter:
```bash
npm install -D @sveltejs/adapter-static
```

2. Update `svelte.config.js`:
```javascript
import adapter from '@sveltejs/adapter-static';

export default {
  kit: {
    adapter: adapter()
  }
};
```

3. Build:
```bash
npm run build
```

Output: `build/` directory with static files

## Deployment

### Vercel (Recommended)

```bash
# Install Vercel CLI
npm i -g vercel

# Deploy
vercel
```

### Netlify

```bash
# Install Netlify CLI
npm i -g netlify-cli

# Deploy
netlify deploy --prod
```

### Docker

```dockerfile
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
EXPOSE 3000
CMD ["node", "build"]
```

```bash
docker build -t archiving-frontend .
docker run -p 3000:3000 archiving-frontend
```

### Environment Variables

For production, set:

```env
VITE_GRAPHQL_URI=https://api.yourdomain.com/graphql
NODE_ENV=production
```

## Troubleshooting

### Backend Connection Failed

**Issue**: `Failed to fetch` or `Network error`

**Solution**:
1. Ensure backend is running: http://localhost:2020/graphql
2. Check CORS configuration in backend
3. Verify `VITE_GRAPHQL_URI` in `.env`

### Apollo Cache Warning

**Issue**: `Cache data may be lost when replacing the getAllUsers field`

**Solution**: Already fixed with merge functions in `apollo.ts`. If you see this:
1. Check `typePolicies` configuration
2. Ensure all list queries have merge functions
3. Clear browser cache

### Module Not Found

**Issue**: `Cannot find module '@/lib/something'`

**Solution**:
1. Check import path (use `$lib` alias)
2. Run `npm install`
3. Restart dev server

### Build Errors

**Issue**: Build fails with TypeScript errors

**Solution**:
```bash
# Type check
npm run check

# Fix common issues
npm run lint:fix

# Clean and rebuild
rm -rf .svelte-kit node_modules
npm install
npm run build
```

## Performance Optimization

### 1. Code Splitting

SvelteKit automatically code-splits by route:
- Each route is a separate bundle
- Loaded on-demand

### 2. Image Optimization

```html
<!-- Use modern formats -->
<img src="/images/logo.webp" alt="Logo" loading="lazy" />

<!-- Responsive images -->
<picture>
  <source srcset="/images/hero.webp" type="image/webp" />
  <img src="/images/hero.jpg" alt="Hero" />
</picture>
```

### 3. Lazy Loading

```svelte
<script>
  import { onMount } from 'svelte';
  
  let Component;
  onMount(async () => {
    Component = (await import('./HeavyComponent.svelte')).default;
  });
</script>

{#if Component}
  <svelte:component this={Component} />
{/if}
```

### 4. Preloading

```typescript
// routes/archives/+page.ts
export const load = async ({ fetch }) => {
  const response = await fetch('/api/archives');
  return {
    archives: await response.json()
  };
};
```

## Testing

### Unit Tests (Vitest)

```bash
npm run test
```

Example test:
```typescript
import { describe, it, expect } from 'vitest';
import { toasts } from '$lib/stores/toastStore';

describe('Toast Store', () => {
  it('adds toast', () => {
    const id = toasts.add('Test', 'success');
    expect(id).toBeDefined();
  });
});
```

### E2E Tests (Playwright)

```bash
npm run test:e2e
```

Example test:
```typescript
import { test, expect } from '@playwright/test';

test('creates user', async ({ page }) => {
  await page.goto('/users/create');
  await page.fill('[name="name"]', 'Test User');
  await page.fill('[name="email"]', 'test@example.com');
  await page.click('button[type="submit"]');
  await expect(page).toHaveURL('/users');
});
```

## Security

### XSS Protection

Svelte automatically escapes HTML:
```svelte
<!-- Safe -->
<p>{userInput}</p>

<!-- Unsafe (use only for trusted content) -->
<p>{@html trustedHTML}</p>
```

### CSRF Protection

GraphQL mutations are protected by:
1. Same-origin policy
2. CORS configuration
3. Content-Type checks

### Input Validation

Always validate on both frontend and backend:
```typescript
function validateEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}
```

## Accessibility

### ARIA Labels

```svelte
<button aria-label="Delete user">
  <TrashIcon />
</button>
```

### Keyboard Navigation

All interactive elements are keyboard accessible:
- Tab to focus
- Enter/Space to activate
- Esc to close modals

### Screen Reader Support

```svelte
<div role="alert" aria-live="polite">
  {message}
</div>
```

## Useful Commands

```bash
# Development
npm run dev          # Start dev server
npm run build        # Build for production
npm run preview      # Preview production build

# Quality
npm run check        # Type check
npm run lint         # Lint code
npm run format       # Format code

# Testing
npm run test         # Unit tests
npm run test:e2e     # E2E tests

# Dependencies
npm install          # Install dependencies
npm update           # Update dependencies
npm audit            # Check for vulnerabilities
```

## Resources

- **SvelteKit**: https://kit.svelte.dev/
- **Svelte**: https://svelte.dev/
- **Apollo Client**: https://www.apollographql.com/docs/react/
- **Vite**: https://vitejs.dev/
- **TypeScript**: https://www.typescriptlang.org/

## Browser Support

- Chrome/Edge: Latest 2 versions
- Firefox: Latest 2 versions
- Safari: Latest 2 versions
- Mobile: iOS Safari 14+, Chrome Android

## Common Patterns

### Form Handling

```svelte
<script>
  let formData = { name: '', email: '' };
  
  async function handleSubmit() {
    // Validate
    if (!formData.name || !formData.email) return;
    
    // Submit
    try {
      await client.mutate({
        mutation: CREATE_USER,
        variables: { input: formData }
      });
      toasts.add('Success!', 'success');
      goto('/users');
    } catch (e) {
      toasts.add(`Error: ${e.message}`, 'error');
    }
  }
</script>

<form on:submit|preventDefault={handleSubmit}>
  <input bind:value={formData.name} />
  <input bind:value={formData.email} />
  <button type="submit">Submit</button>
</form>
```

### Loading States

```svelte
<script>
  let loading = true;
  let data = [];
  
  onMount(async () => {
    try {
      const result = await client.query({ query: GET_DATA });
      data = result.data.items;
    } finally {
      loading = false;
    }
  });
</script>

{#if loading}
  <div class="loading">Loading...</div>
{:else}
  {#each data as item}
    <div>{item.name}</div>
  {/each}
{/if}
```

## Support

For issues or questions:
1. Check browser console for errors
2. Verify backend is running and accessible
3. Check network tab for failed requests
4. Review Apollo Client cache state

---

**Last Updated**: February 1, 2026  
**Framework**: SvelteKit  
**Node Version**: 20+  
**Package Manager**: npm/pnpm
