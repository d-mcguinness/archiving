<script lang="ts">
  import { client } from '$lib/apollo';
  import { apiFetch } from '$lib/api';
  import { auth } from '$lib/stores/authStore';
  import { roleLabel } from '$lib/roles';
  import { standards, graphqlToKey } from '$lib/standards';
  import {
    GET_ALL_ARCHIVES,
    GET_ARCHIVES_BY_TENANT,
    GET_ARCHIVES_BY_USER_ASSIGNMENT,
    GET_ALL_TENANTS,
    GET_ALL_USERS
  } from '$lib/graphql/queries';
  import {
    searchScopesFor,
    applySearch,
    emptySearchValues,
    ARCHIVE_STATUSES,
    TENANT_STATUSES,
    USER_ROLES,
    type ScopeDefinition,
    type SearchScope,
    type SearchField
  } from '$lib/search';

  // The inputs on this page come from the signed-in role (see $lib/search).
  $: role = $auth.role;
  $: scopes = searchScopesFor(role);

  let scope: SearchScope | '' = '';
  $: if (scopes.length && !scopes.some((s) => s.scope === scope)) scope = scopes[0].scope;
  $: definition = scopes.find((s) => s.scope === scope) as ScopeDefinition | undefined;
  $: offers = (field: SearchField) => !!definition?.fields.includes(field);

  let values = emptySearchValues();

  // Options for the tenant / archive pickers, loaded once per role.
  let tenants: any[] = [];
  let archives: any[] = [];
  let optionsFor = '';
  $: if (role && optionsFor !== role) {
    optionsFor = role;
    loadOptions();
  }

  let results: any[] | null = null;
  let resultsScope: SearchScope | '' = '';
  let searching = false;
  let error: string | null = null;

  const pretty = (value: string) =>
    value ? value.charAt(0) + value.slice(1).toLowerCase().replace(/_/g, ' ') : '';
  const formatDate = (value: string | null | undefined) =>
    value ? new Date(value).toLocaleDateString() : '-';
  const tenantName = (id: unknown) => {
    const tenant = tenants.find((t) => String(t.id) === String(id));
    return tenant ? tenant.displayName || tenant.name : '-';
  };
  const archiveTitle = (id: unknown) =>
    archives.find((a) => String(a.id) === String(id))?.title ?? '-';

  async function query(document: any, field: string, variables?: Record<string, string>) {
    const result = await client.query({ query: document, variables, fetchPolicy: 'network-only' });
    return result?.data?.[field] || [];
  }

  /** Archives the signed-in role may see. */
  function fetchArchives(): Promise<any[]> {
    const { tenantId, user } = $auth;
    if (role === 'ADMIN') return query(GET_ALL_ARCHIVES, 'getAllArchives');
    if (role === 'TENANT' && tenantId) {
      return query(GET_ARCHIVES_BY_TENANT, 'getArchivesByTenant', { tenantId: String(tenantId) });
    }
    if (role === 'USER' && user?.id) {
      return query(GET_ARCHIVES_BY_USER_ASSIGNMENT, 'getArchivesByUserAssignment', {
        userId: String(user.id)
      });
    }
    return Promise.resolve([]);
  }

  /** Documents the signed-in role may see; the backend scopes them to the caller. */
  async function fetchDocuments(): Promise<any[]> {
    const response = await apiFetch('/api/documents');
    if (!response.ok) throw new Error('Failed to load documents');
    const body = await response.json();
    if (!body.success) throw new Error(body.message || 'Failed to load documents');
    const documents: any[] = body.documents || [];
    // A member searches their own documents, not the whole organization's.
    return role === 'USER'
      ? documents.filter((d) => String(d.userId) === String($auth.user?.id))
      : documents;
  }

  async function loadOptions() {
    try {
      const [loadedTenants, loadedArchives] = await Promise.all([
        role === 'ADMIN' ? query(GET_ALL_TENANTS, 'getAllTenants') : Promise.resolve([]),
        fetchArchives()
      ]);
      tenants = loadedTenants;
      archives = loadedArchives;
    } catch (e) {
      // The pickers stay empty; searching by the other inputs still works.
      console.error('Failed to load search options:', e);
    }
  }

  async function handleSearch() {
    if (!definition) return;
    const target = definition;
    searching = true;
    error = null;
    try {
      let items: any[] = [];
      if (target.scope === 'archives') items = await fetchArchives();
      else if (target.scope === 'documents') items = await fetchDocuments();
      else if (target.scope === 'tenants') items = await query(GET_ALL_TENANTS, 'getAllTenants');
      else if (target.scope === 'users') items = await query(GET_ALL_USERS, 'getAllUsers');
      results = applySearch(target, items, values);
      resultsScope = target.scope;
    } catch (e) {
      error = e instanceof Error ? e.message : 'Search failed';
      results = null;
    } finally {
      searching = false;
    }
  }

  function handleReset() {
    values = emptySearchValues();
    results = null;
    error = null;
  }

  function selectScope(next: SearchScope) {
    scope = next;
    results = null;
    error = null;
  }
</script>

<svelte:head>
  <title>Search - Arcana</title>
</svelte:head>

<div class="search-page">
  <div class="page-heading">
    <span class="eyebrow">{roleLabel(role)}</span>
    <h1 class="page-title">Search</h1>
    <p class="page-sub">
      {#if role === 'ADMIN'}
        Search every tenant's archives and documents, or find a tenant or user.
      {:else if role === 'TENANT'}
        Search your organization's archives and documents.
      {:else}
        Search the documents you have uploaded.
      {/if}
    </p>
  </div>

  {#if definition}
    <form class="form-container search-form" on:submit|preventDefault={handleSearch}>
      {#if scopes.length > 1}
        <div class="scope-tabs" role="group" aria-label="Search in">
          {#each scopes as option (option.scope)}
            <button
              type="button"
              class="scope-tab"
              class:selected={option.scope === scope}
              aria-pressed={option.scope === scope}
              on:click={() => selectScope(option.scope)}
            >
              {option.label}
            </button>
          {/each}
        </div>
      {/if}

      <div class="fields">
        <div class="field wide">
          <label for="search-keyword">Keyword</label>
          <!-- svelte-ignore a11y-autofocus -->
          <input
            id="search-keyword"
            type="search"
            placeholder={definition.keywordHint}
            bind:value={values.keyword}
            autofocus
          />
        </div>

        {#if offers('tenant')}
          <div class="field">
            <label for="search-tenant">Tenant</label>
            <select id="search-tenant" bind:value={values.tenantId}>
              <option value="">All tenants</option>
              {#each tenants as tenant (tenant.id)}
                <option value={String(tenant.id)}>{tenant.displayName || tenant.name}</option>
              {/each}
            </select>
          </div>
        {/if}

        {#if offers('archive')}
          <div class="field">
            <label for="search-archive">Archive</label>
            <select id="search-archive" bind:value={values.archiveId}>
              <option value="">{role === 'USER' ? 'All my archives' : 'All archives'}</option>
              {#each archives as archive (archive.id)}
                <option value={String(archive.id)}>{archive.title}</option>
              {/each}
            </select>
          </div>
        {/if}

        {#if offers('archiveStatus')}
          <div class="field">
            <label for="search-status">Status</label>
            <select id="search-status" bind:value={values.archiveStatus}>
              <option value="">Any status</option>
              {#each ARCHIVE_STATUSES as status}
                <option value={status}>{pretty(status)}</option>
              {/each}
            </select>
          </div>
        {/if}

        {#if offers('standard')}
          <div class="field">
            <label for="search-standard">Standard</label>
            <select id="search-standard" bind:value={values.standard}>
              <option value="">Any standard</option>
              {#each standards as standard (standard.graphql)}
                <option value={standard.graphql}>{standard.label}</option>
              {/each}
            </select>
          </div>
        {/if}

        {#if offers('tenantStatus')}
          <div class="field">
            <label for="search-tenant-status">Status</label>
            <select id="search-tenant-status" bind:value={values.tenantStatus}>
              <option value="">Any status</option>
              {#each TENANT_STATUSES as status}
                <option value={status}>{pretty(status)}</option>
              {/each}
            </select>
          </div>
        {/if}

        {#if offers('userRole')}
          <div class="field">
            <label for="search-role">Role</label>
            <select id="search-role" bind:value={values.userRole}>
              <option value="">Any role</option>
              {#each USER_ROLES as userRole}
                <option value={userRole}>{roleLabel(userRole)}</option>
              {/each}
            </select>
          </div>
        {/if}

        {#if offers('dateRange')}
          <div class="field">
            <label for="search-from">{scope === 'documents' ? 'Uploaded from' : 'Created from'}</label>
            <input id="search-from" type="date" bind:value={values.from} max={values.to || undefined} />
          </div>
          <div class="field">
            <label for="search-to">{scope === 'documents' ? 'Uploaded to' : 'Created to'}</label>
            <input id="search-to" type="date" bind:value={values.to} min={values.from || undefined} />
          </div>
        {/if}
      </div>

      <div class="actions">
        <button type="button" class="btn-secondary" on:click={handleReset}>Clear</button>
        <button type="submit" class="btn-primary" disabled={searching}>
          {searching ? 'Searching…' : 'Search'}
        </button>
      </div>
    </form>
  {/if}

  {#if error}
    <div class="error" role="alert">{error}</div>
  {/if}

  <div aria-live="polite">
    {#if results && !searching}
      {#if results.length === 0}
        <div class="no-results">
          <h2>No matches</h2>
          <p>Nothing matches these filters. Try a different keyword or clear some of them.</p>
        </div>
      {:else}
        <p class="result-count">{results.length} {results.length === 1 ? 'result' : 'results'}</p>
        <div class="table-card results">
          <table class="arc-table">
            {#if resultsScope === 'archives'}
              <thead>
                <tr>
                  <th>Title</th>
                  {#if role === 'ADMIN'}<th>Tenant</th>{/if}
                  <th>Status</th>
                  <th>Standard</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {#each results as archive (archive.id)}
                  <tr>
                    <td>
                      <a class="result-link" href="/tenants/{archive.tenantId}/archives/{archive.id}">{archive.title}</a>
                      {#if archive.description}<div class="result-sub">{archive.description}</div>{/if}
                    </td>
                    {#if role === 'ADMIN'}
                      <td>{archive.tenant?.displayName || archive.tenant?.name || tenantName(archive.tenantId)}</td>
                    {/if}
                    <td><span class="badge {archive.status.toLowerCase()}">{archive.status}</span></td>
                    <td><span class="badge cyan">{graphqlToKey[archive.standard] ?? archive.standard}</span></td>
                    <td>{formatDate(archive.createdAt)}</td>
                  </tr>
                {/each}
              </tbody>
            {:else if resultsScope === 'documents'}
              <thead>
                <tr>
                  <th>Title</th>
                  <th>File</th>
                  {#if role === 'ADMIN'}<th>Tenant</th>{:else}<th>Archive</th>{/if}
                  <th>Status</th>
                  <th>Uploaded</th>
                </tr>
              </thead>
              <tbody>
                {#each results as doc (doc.id)}
                  <tr>
                    <td>
                      <a class="result-link" href="/tenants/{doc.tenantId}/users/{doc.userId}/documents/{doc.id}">{doc.title || doc.fileName}</a>
                      {#if doc.description}<div class="result-sub">{doc.description}</div>{/if}
                    </td>
                    <td>{doc.fileName}</td>
                    {#if role === 'ADMIN'}
                      <td>{tenantName(doc.tenantId)}</td>
                    {:else}
                      <td>{doc.archiveId ? archiveTitle(doc.archiveId) : '-'}</td>
                    {/if}
                    <td><span class="badge slate">{doc.status || 'Unknown'}</span></td>
                    <td>{formatDate(doc.uploadedAt || doc.createdAt)}</td>
                  </tr>
                {/each}
              </tbody>
            {:else if resultsScope === 'tenants'}
              <thead>
                <tr>
                  <th>Tenant</th>
                  <th>Domain</th>
                  <th>Status</th>
                  <th>Plan</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {#each results as tenant (tenant.id)}
                  <tr>
                    <td>
                      <a class="result-link" href="/tenants/{tenant.id}">{tenant.displayName || tenant.name}</a>
                      {#if tenant.description}<div class="result-sub">{tenant.description}</div>{/if}
                    </td>
                    <td>{tenant.domain || '-'}</td>
                    <td><span class="badge {tenant.status.toLowerCase()}">{pretty(tenant.status)}</span></td>
                    <td>{pretty(tenant.plan || '') || '-'}</td>
                    <td>{formatDate(tenant.createdAt)}</td>
                  </tr>
                {/each}
              </tbody>
            {:else if resultsScope === 'users'}
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                </tr>
              </thead>
              <tbody>
                {#each results as user (user.id)}
                  <tr>
                    <td><a class="result-link" href="/users/update?userId={user.id}">{user.name}</a></td>
                    <td>{user.email}</td>
                    <td><span class="badge indigo">{roleLabel(user.role)}</span></td>
                  </tr>
                {/each}
              </tbody>
            {/if}
          </table>
        </div>
      {/if}
    {/if}
  </div>
</div>

<style>
  .search-page {
    max-width: 1100px;
    margin: 0 auto;
    padding: 2rem 1rem 3rem;
  }

  .page-heading {
    margin-bottom: 1.5rem;
  }

  .page-title {
    font-size: 2rem;
    margin: 0 0 0.4rem;
  }

  .page-sub {
    margin: 0;
  }

  .search-form {
    padding: 1.5rem;
  }

  .scope-tabs {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    margin-bottom: 1.25rem;
    padding-bottom: 1.25rem;
    border-bottom: 1px solid var(--arc-line);
  }

  .scope-tab {
    padding: 0.5rem 1rem;
    font: inherit;
    font-size: 0.9rem;
    font-weight: 600;
    color: var(--arc-body);
    background: transparent;
    border: 1.5px solid var(--arc-line-strong);
    border-radius: 999px;
    box-shadow: none;
    cursor: pointer;
    transition: background 0.18s ease, border-color 0.18s ease, color 0.18s ease;
  }

  .scope-tab:hover {
    border-color: var(--arc-hover-border);
    color: var(--arc-ink);
  }

  .scope-tab.selected {
    background: var(--arc-chip-indigo-bg);
    border-color: var(--arc-chip-indigo-bg);
    color: var(--arc-chip-indigo-ink);
  }

  .fields {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(210px, 1fr));
    gap: 1rem;
  }

  .field.wide {
    grid-column: 1 / -1;
  }

  .actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.75rem;
    margin-top: 1.5rem;
  }

  .result-count {
    margin: 0 0 0.75rem;
    font-weight: 600;
    color: var(--arc-muted);
  }

  .results {
    overflow-x: auto;
  }

  .results td {
    padding: 0.85rem 1rem;
    border-bottom: 1px solid var(--arc-line);
    color: var(--arc-body);
    vertical-align: top;
  }

  .results tr:last-child td {
    border-bottom: none;
  }

  .result-link {
    font-weight: 600;
    color: var(--arc-link);
    text-decoration: none;
  }

  .result-link:hover {
    text-decoration: underline;
  }

  .result-sub {
    margin-top: 0.2rem;
    font-size: 0.85rem;
    color: var(--arc-muted);
  }

  .no-results {
    padding: 2.5rem 1rem;
    text-align: center;
    color: var(--arc-muted);
    background: var(--arc-card);
    border: 1px solid var(--arc-line);
    border-radius: 1rem;
  }

  .no-results h2 {
    margin: 0 0 0.4rem;
    font-size: 1.15rem;
    color: var(--arc-ink);
  }

  .no-results p {
    margin: 0;
  }
</style>
