<script lang="ts">
  import { auth } from '$lib/stores/authStore';
  import { goto } from '$app/navigation';
  import { browser } from '$app/environment';

  $: if (browser && $auth.isLoggedIn && $auth.role === 'ADMIN') {
    goto('/admin/documents');
  }
  $: if (browser && $auth.isLoggedIn && $auth.role === 'TENANT' && $auth.tenantId) {
    goto(`/tenants/${$auth.tenantId}/documents`);
  }
  $: if (browser && $auth.isLoggedIn && $auth.role === 'USER' && $auth.tenantId && $auth.user?.id) {
    goto(`/tenants/${$auth.tenantId}/users/${$auth.user.id}/documents`);
  }
</script>

<svelte:head>
  <title>Documents - Arcana</title>
</svelte:head>

{#if !$auth.isLoggedIn}
  <div class="public-page">
    <section class="hero">
      <div class="hero-badge">Cloud Storage</div>
      <h1>Document Management</h1>
      <p class="hero-subtitle">
        Upload, organize, and manage documents within your archival workflows. Attach files to
        archives, Intakes, Preservations, and Releases with secure cloud storage and role-based access.
      </p>
      <div class="hero-actions">
        <a href="/login" class="btn-cta">Sign In</a>
        <a href="#features" class="btn-outline">Learn More</a>
      </div>
    </section>

    <section id="features" class="features-section">
      <h2 class="section-title">Document Features</h2>
      <div class="features-grid">
        <div class="feature-card">
          <span class="feature-icon">📄</span>
          <h3>File Upload</h3>
          <p>Upload any file type with automatic content detection, size tracking, and metadata extraction.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">🔗</span>
          <h3>Package Linking</h3>
          <p>Associate documents with archives, Intakes, Preservations, or Releases for complete package assembly.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">☁️</span>
          <h3>Cloud Storage</h3>
          <p>Secure S3-compatible storage with presigned URLs for reliable upload and download.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">🔒</span>
          <h3>Access Control</h3>
          <p>Tenant-scoped documents with role-based visibility. Users see only their own uploads.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">📊</span>
          <h3>Status Tracking</h3>
          <p>Track document lifecycle from upload through review, approval, and archival.</p>
        </div>
        <div class="feature-card">
          <span class="feature-icon">📥</span>
          <h3>Direct Download</h3>
          <p>Stream files directly to the browser with proper content types and filenames.</p>
        </div>
      </div>
    </section>

    <section class="cta-section">
      <h2>Start managing your documents</h2>
      <p>Sign in to upload and organize your files.</p>
      <a href="/login" class="btn-cta-inv">Get Started</a>
    </section>
  </div>
{/if}

<style>
  .public-page { text-align: center; }
  .hero { padding: 4rem 2rem 3rem; }

  .hero-badge {
    display: inline-block; padding: 0.4rem 1rem;
    background: #f5f3ff; color: #7c3aed;
    border-radius: 2rem; font-size: 0.8rem; font-weight: 700;
    text-transform: uppercase; letter-spacing: 0.05em; margin-bottom: 1.25rem;
  }

  .hero h1 { font-size: 3rem; font-weight: 800; color: #0f172a; margin: 0 0 1rem; letter-spacing: -0.03em; }
  .hero-subtitle { font-size: 1.1rem; color: #475569; max-width: 620px; margin: 0 auto 2.5rem; line-height: 1.7; }
  .hero-actions { display: flex; justify-content: center; gap: 1rem; flex-wrap: wrap; }

  .btn-cta, .btn-cta-inv {
    display: inline-block; padding: 0.85rem 2rem;
    background: linear-gradient(135deg, #8b5cf6, #7c3aed);
    color: white; border-radius: 0.5rem; text-decoration: none;
    font-weight: 700; font-size: 0.95rem;
    transition: transform 0.2s, box-shadow 0.2s;
  }
  .btn-cta:hover, .btn-cta-inv:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(139, 92, 246, 0.35); }

  .btn-outline {
    display: inline-block; padding: 0.85rem 2rem;
    background: transparent; color: #1e293b; border: 2px solid #cbd5e1;
    border-radius: 0.5rem; text-decoration: none; font-weight: 700; font-size: 0.95rem;
    transition: border-color 0.2s;
  }
  .btn-outline:hover { border-color: #64748b; }

  .section-title { font-size: 1.75rem; font-weight: 800; color: #0f172a; margin: 0 0 2rem; }
  .features-section { margin-bottom: 4rem; }

  .features-grid {
    display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 1.25rem; text-align: left;
  }

  .feature-card {
    background: white; border: 1px solid #e2e8f0; border-radius: 0.75rem;
    padding: 1.5rem; transition: border-color 0.2s;
  }
  .feature-card:hover { border-color: #8b5cf6; }
  .feature-icon { font-size: 2rem; display: block; margin-bottom: 0.75rem; }
  .feature-card h3 { margin: 0 0 0.5rem; color: #0f172a; font-size: 1.05rem; }
  .feature-card p { margin: 0; color: #64748b; font-size: 0.875rem; line-height: 1.55; }

  .cta-section {
    background: linear-gradient(135deg, #1e293b, #334155); color: white;
    padding: 3.5rem 2rem; border-radius: 1rem; margin-bottom: 2rem;
  }
  .cta-section h2 { margin: 0 0 0.5rem; font-size: 1.75rem; font-weight: 800; }
  .cta-section p { margin: 0 0 2rem; color: #94a3b8; font-size: 1.05rem; }
  .cta-section .btn-cta-inv { background: white; color: #1e293b; }
  .cta-section .btn-cta-inv:hover { box-shadow: 0 8px 24px rgba(255, 255, 255, 0.15); }

  @media (max-width: 640px) { .hero h1 { font-size: 2.25rem; } }
</style>
