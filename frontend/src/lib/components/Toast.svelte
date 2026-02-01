<script lang="ts">
  import { toasts } from '$lib/stores/toastStore';
  import { fly, fade } from 'svelte/transition';

  function getIcon(type: string) {
    switch (type) {
      case 'success': return '✓';
      case 'error': return '✕';
      case 'warning': return '⚠';
      case 'info': return 'ℹ';
      default: return 'ℹ';
    }
  }
</script>

<div class="toast-container">
  {#each $toasts as toast (toast.id)}
    <div
      class="toast toast-{toast.type}"
      transition:fly="{{ y: -50, duration: 300 }}"
      role="alert"
    >
      <span class="toast-icon">{getIcon(toast.type)}</span>
      <span class="toast-message">{toast.message}</span>
      <button
        class="toast-close"
        on:click={() => toasts.remove(toast.id)}
        aria-label="Close"
      >
        ×
      </button>
    </div>
  {/each}
</div>

<style>
  .toast-container {
    position: fixed;
    top: 1rem;
    right: 1rem;
    z-index: 9999;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    max-width: 400px;
  }

  .toast {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 1rem 1.25rem;
    border-radius: 0.5rem;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    font-size: 0.875rem;
    font-weight: 500;
    min-width: 300px;
  }

  .toast-success {
    background: #10b981;
    color: white;
  }

  .toast-error {
    background: #ef4444;
    color: white;
  }

  .toast-warning {
    background: #f59e0b;
    color: white;
  }

  .toast-info {
    background: #3b82f6;
    color: white;
  }

  .toast-icon {
    font-size: 1.25rem;
    font-weight: bold;
    flex-shrink: 0;
  }

  .toast-message {
    flex: 1;
    line-height: 1.5;
  }

  .toast-close {
    background: none;
    border: none;
    color: inherit;
    font-size: 1.5rem;
    cursor: pointer;
    padding: 0;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 0.25rem;
    transition: background 0.2s;
    flex-shrink: 0;
  }

  .toast-close:hover {
    background: rgba(255, 255, 255, 0.2);
  }

  @media (max-width: 640px) {
    .toast-container {
      left: 1rem;
      right: 1rem;
      max-width: none;
    }

    .toast {
      min-width: auto;
    }
  }
</style>
