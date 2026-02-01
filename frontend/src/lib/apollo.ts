// Use namespace imports for consistent interop in Vite SSR
import * as apolloCore from '@apollo/client/core';
import * as apolloLinkError from '@apollo/client/link/error';
import type { ErrorResponse, ServerError, ServerParseError } from '@apollo/client/link/error';

const { ApolloClient, InMemoryCache, createHttpLink, ApolloLink } = apolloCore;
const { onError } = apolloLinkError;

const uri: string = import.meta.env?.VITE_GRAPHQL_URI || '/graphql';

const httpLink = createHttpLink({
  uri,
  fetchOptions: { mode: 'cors' },
  credentials: 'include'
});

const errorLink = onError(({ graphQLErrors, networkError, operation }: ErrorResponse) => {
  const parts: string[] = [];

  if (graphQLErrors && graphQLErrors.length) {
    parts.push(...graphQLErrors.map(e => e?.message).filter(Boolean));
  }

  if (networkError) {
    // Handle different types of network errors with proper type checking
    const serverError = networkError as ServerError;
    const parseError = networkError as ServerParseError;

    // Check for server errors with result property
    if ('result' in serverError &&
        serverError.result &&
        typeof serverError.result === 'object' &&
        'errors' in serverError.result &&
        Array.isArray(serverError.result.errors)) {
      const neMsgs = serverError.result.errors
        .map((e: any) => e?.message)
        .filter(Boolean) || [];
      if (neMsgs.length) parts.push(...neMsgs);
    }

    // Check for HTTP status codes
    if ('statusCode' in serverError && serverError.statusCode) {
      parts.push(`HTTP ${serverError.statusCode}`);
    }

    // Add general network error message if no specific errors found
    if (networkError?.message &&
        !('result' in serverError &&
          serverError.result &&
          typeof serverError.result === 'object' &&
          'errors' in serverError.result &&
          Array.isArray(serverError.result.errors) &&
          serverError.result.errors.length)) {
      parts.push(networkError.message);
    }
  }

  const msg = parts.join('; ') || 'Unknown error';

  // Log full error context for debugging
  console.error('Apollo error:', msg, `(${operation?.operationName || 'anonymous'})`, {
    graphQLErrors,
    networkError
  });
});

const link = ApolloLink.from([errorLink, httpLink]);

interface CacheTypePolicies {
  Query: {
    fields: {
      getAllUsers: {
        merge(existing?: any[], incoming?: any[]): any[];
      };
      getAllTenants: {
        merge(existing?: any[], incoming?: any[]): any[];
      };
      getAllArchives: {
        merge(existing?: any[], incoming?: any[]): any[];
      };
      getElementsByArchive: {
        merge(existing?: any[], incoming?: any[]): any[];
      };
    };
  };
  Tenant: { keyFields: string[] };
  Archive: { keyFields: string[] };
  User: { keyFields: string[] };
  Element: { keyFields: string[] };
  TenantSettings: { keyFields: boolean };
}

const cache = new InMemoryCache({
  typePolicies: {
    Query: {
      fields: {
        getAllUsers: {
          merge(existing: any[] = [], incoming: any[] = []): any[] {
            // Always use the incoming data (fresh from server)
            // This ensures the cache is updated correctly after mutations like delete
            return incoming;
          }
        },
        getAllTenants: {
          merge(existing: any[] = [], incoming: any[] = []): any[] {
            return incoming;
          }
        },
        getAllArchives: {
          merge(existing: any[] = [], incoming: any[] = []): any[] {
            // Always use the incoming data (fresh from server)
            // This ensures the cache is updated correctly after mutations like delete
            return incoming;
          }
        },
        getElementsByArchive: {
          merge(existing: any[] = [], incoming: any[] = []): any[] {
            // Always use the incoming data (fresh from server)
            // This ensures the cache is updated correctly after mutations like delete
            return incoming;
          }
        }
      }
    },
    Tenant: { keyFields: ['id'] },
    Archive: { keyFields: ['id'] },
    User: { keyFields: ['id'] },
    Element: { keyFields: ['id'] },
    TenantSettings: { keyFields: false }
  }
});

export const client = new ApolloClient({
  link,
  cache,
  defaultOptions: {
    watchQuery: {
      errorPolicy: 'all',
      fetchPolicy: 'cache-and-network'
    },
    query: {
      errorPolicy: 'all',
      fetchPolicy: 'no-cache'
    }
  }
});
