import { gql } from '@apollo/client/core';
import type { DocumentNode } from '@apollo/client/core';

// Dashboard Queries
export const GET_DASHBOARD_STATS: DocumentNode = gql`
  query GetDashboardStats {
    getDashboardStats {
      totalUsers
      totalTenants
      totalArchives
      activeArchives
      draftArchives
      archivedArchives
    }
  }
`;

export const GET_TENANT_DASHBOARD_STATS: DocumentNode = gql`
  query GetTenantDashboardStats($tenantId: ID!) {
    getTenantDashboardStats(tenantId: $tenantId) {
      tenantId
      tenantName
      tenantStatus
      tenantPlan
      totalUsers
      totalArchives
      activeArchives
      draftArchives
      archivedArchives
    }
  }
`;

// User Queries
export const GET_ALL_USERS: DocumentNode = gql`
  query GetAllUsers {
    getAllUsers {
      id
      name
      email
      age
    }
  }
`;

export const GET_USER: DocumentNode = gql`
  query GetUser($id: ID!) {
    getUser(id: $id) {
      id
      name
      email
      age
    }
  }
`;

export const CREATE_USER: DocumentNode = gql`
  mutation CreateUser($input: CreateUserInput!) {
    createUser(input: $input) {
      id
      name
      email
      age
    }
  }
`;

export const UPDATE_USER: DocumentNode = gql`
  mutation UpdateUser($id: ID!, $input: CreateUserInput!) {
    updateUser(id: $id, input: $input) {
      id
      name
      email
      age
    }
  }
`;

export const DELETE_USER: DocumentNode = gql`
  mutation DeleteUser($id: ID!) {
    deleteUser(id: $id)
  }
`;

// Tenant Queries
export const GET_ALL_TENANTS: DocumentNode = gql`
  query GetAllTenants {
    getAllTenants {
      id
      name
      domain
      displayName
      description
      status
      plan
      createdAt
      updatedAt
      ownerId
      settings {
        maxUsers
        maxArchives
        maxStorageBytes
        allowExternalSharing
        enableAuditLog
        timezone
        defaultLanguage
        customDomain
      }
    }
  }
`;

export const GET_TENANT: DocumentNode = gql`
  query GetTenant($id: ID!) {
    getTenant(id: $id) {
      id
      name
      domain
      displayName
      description
      status
      plan
      createdAt
      updatedAt
      ownerId
      settings {
        maxUsers
        maxArchives
        maxStorageBytes
        allowExternalSharing
        enableAuditLog
        timezone
        defaultLanguage
        customDomain
      }
    }
  }
`;

export const GET_TENANTS_BY_STATUS: DocumentNode = gql`
  query GetTenantsByStatus($status: TenantStatus!) {
    getTenantsByStatus(status: $status) {
      id
      name
      domain
      displayName
      description
      status
      plan
      createdAt
      updatedAt
      ownerId
    }
  }
`;

export const GET_TENANTS_BY_OWNER: DocumentNode = gql`
  query GetTenantsByOwner($ownerId: ID!) {
    getTenantsByOwner(ownerId: $ownerId) {
      id
      name
      domain
      displayName
      description
      status
      plan
      createdAt
      updatedAt
      ownerId
    }
  }
`;

export const CREATE_TENANT: DocumentNode = gql`
  mutation CreateTenant($input: CreateTenantInput!) {
    createTenant(input: $input) {
      id
      name
      domain
      displayName
      description
      status
      plan
      createdAt
      updatedAt
      ownerId
      settings {
        maxUsers
        maxArchives
        maxStorageBytes
        allowExternalSharing
        enableAuditLog
        timezone
        defaultLanguage
        customDomain
      }
    }
  }
`;

export const UPDATE_TENANT: DocumentNode = gql`
  mutation UpdateTenant($input: UpdateTenantInput!) {
    updateTenant(input: $input) {
      id
      name
      domain
      displayName
      description
      status
      plan
      createdAt
      updatedAt
      ownerId
      settings {
        maxUsers
        maxArchives
        maxStorageBytes
        allowExternalSharing
        enableAuditLog
        timezone
        defaultLanguage
        customDomain
      }
    }
  }
`;

export const DELETE_TENANT: DocumentNode = gql`
  mutation DeleteTenant($id: ID!) {
    deleteTenant(id: $id)
  }
`;

export const ADD_USER_TO_TENANT: DocumentNode = gql`
  mutation AddUserToTenant($userId: ID!, $tenantId: ID!) {
    addUserToTenant(userId: $userId, tenantId: $tenantId)
  }
`;

export const REMOVE_USER_FROM_TENANT: DocumentNode = gql`
  mutation RemoveUserFromTenant($userId: ID!) {
    removeUserFromTenant(userId: $userId)
  }
`;

// Archive Queries
export const GET_ALL_ARCHIVES: DocumentNode = gql`
  query GetAllArchives {
    getAllArchives {
      id
      tenantId
      ownerId
      tenant {
        id
        name
        displayName
        domain
      }
      title
      description
      content
      createdAt
      updatedAt
      status
      standard
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_ARCHIVE: DocumentNode = gql`
  query GetArchive($id: ID!) {
    getArchive(id: $id) {
      id
      tenantId
      ownerId
      tenant {
        id
        name
        displayName
        domain
      }
      title
      description
      content
      createdAt
      updatedAt
      status
      standard
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_ARCHIVES_BY_USER: DocumentNode = gql`
  query GetArchivesByUser($userId: ID!) {
    getArchivesByUser(userId: $userId) {
      id
      ownerId
      title
      description
      content
      createdAt
      updatedAt
      status
      standard
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_ARCHIVES_BY_OWNER: DocumentNode = gql`
  query GetArchivesByOwner($ownerId: ID!) {
    getArchivesByOwner(ownerId: $ownerId) {
      id
      ownerId
      title
      description
      content
      createdAt
      updatedAt
      status
      standard
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_ALL_SIPS: DocumentNode = gql`
  query GetAllSips {
    getAllSips {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        fields {
          id
          name
          label
          type
          value
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_SIPS_BY_TENANT: DocumentNode = gql`
  query GetSipsByTenant($tenantId: ID!) {
    getSipsByTenant(tenantId: $tenantId) {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        fields {
          id
          name
          label
          type
          value
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_ARCHIVES_BY_TENANT: DocumentNode = gql`
  query GetArchivesByTenant($tenantId: ID!) {
    getArchivesByTenant(tenantId: $tenantId) {
      id
      ownerId
      tenantId
      title
      description
      content
      createdAt
      updatedAt
      status
      standard
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_ARCHIVES_BY_USER_ASSIGNMENT: DocumentNode = gql`
  query GetArchivesByUserAssignment($userId: ID!) {
    getArchivesByUserAssignment(userId: $userId) {
      id
      ownerId
      title
      description
      content
      createdAt
      updatedAt
      status
      standard
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const CREATE_ARCHIVE: DocumentNode = gql`
  mutation CreateArchive($input: CreateArchiveInput!) {
    createArchive(input: $input) {
      id
      ownerId
      title
      description
      content
      createdAt
      updatedAt
      status
      standard
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const UPDATE_ARCHIVE_STATUS: DocumentNode = gql`
  mutation UpdateArchiveStatus($archiveId: ID!, $status: ArchiveStatus!) {
    updateArchiveStatus(archiveId: $archiveId, status: $status) {
      id
      ownerId
      title
      description
      content
      createdAt
      updatedAt
      status
      standard
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const ASSIGN_USER_TO_ARCHIVE: DocumentNode = gql`
  mutation AssignUserToArchive($input: AssignUserInput!) {
    assignUserToArchive(input: $input) {
      id
      ownerId
      title
      description
      content
      createdAt
      updatedAt
      status
      standard
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const UNASSIGN_USER_FROM_ARCHIVE: DocumentNode = gql`
  mutation UnassignUserFromArchive($input: UnassignUserInput!) {
    unassignUserFromArchive(input: $input) {
      id
      ownerId
      title
      description
      content
      createdAt
      updatedAt
      status
      standard
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

// SIP V2 Queries (dedicated Sip entity)
export const GET_ALL_SIPS_V2: DocumentNode = gql`
  query GetAllSipsV2 {
    getAllSipsV2 {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        fields {
          id
          name
          label
          type
          value
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_SIPS_BY_TENANT_V2: DocumentNode = gql`
  query GetSipsByTenantV2($tenantId: ID!) {
    getSipsByTenantV2(tenantId: $tenantId) {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        fields {
          id
          name
          label
          type
          value
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_SIP: DocumentNode = gql`
  query GetSip($id: ID!) {
    getSip(id: $id) {
      id
      ownerId
      tenantId
      title
      description
      content
      createdAt
      updatedAt
      status
      standard
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        description
        fields {
          id
          name
          label
          type
          value
        }
        children {
          id
          elementIdentifier
          entityName
          entityType
          title
          description
          status
          createdAt
          createdBy
          fields {
            id
            name
            label
            type
            value
          }
          children {
            id
            elementIdentifier
            entityName
            entityType
            title
            status
            fields {
              id
              name
              label
              type
              value
            }
          }
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const CREATE_SIP: DocumentNode = gql`
  mutation CreateSipV2($input: CreateSipInput!) {
    createSipV2(input: $input) {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        fields {
          id
          name
          label
          type
          value
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const DELETE_SIP: DocumentNode = gql`
  mutation DeleteSipV2($id: ID!) {
    deleteSipV2(id: $id)
  }
`;

// AIP Queries
export const GET_ALL_AIPS: DocumentNode = gql`
  query GetAllAips {
    getAllAips {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      sourceSipId
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        fields {
          id
          name
          label
          type
          value
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_AIPS_BY_TENANT: DocumentNode = gql`
  query GetAipsByTenant($tenantId: ID!) {
    getAipsByTenant(tenantId: $tenantId) {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      sourceSipId
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        fields {
          id
          name
          label
          type
          value
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_AIP: DocumentNode = gql`
  query GetAip($id: ID!) {
    getAip(id: $id) {
      id
      ownerId
      tenantId
      title
      description
      content
      createdAt
      updatedAt
      status
      standard
      sourceSipId
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        description
        fields {
          id
          name
          label
          type
          value
        }
        children {
          id
          elementIdentifier
          entityName
          entityType
          title
          description
          status
          createdAt
          createdBy
          fields {
            id
            name
            label
            type
            value
          }
          children {
            id
            elementIdentifier
            entityName
            entityType
            title
            status
            fields {
              id
              name
              label
              type
              value
            }
          }
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const CREATE_AIP: DocumentNode = gql`
  mutation CreateAip($input: CreateAipInput!) {
    createAip(input: $input) {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      sourceSipId
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        fields {
          id
          name
          label
          type
          value
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const DELETE_AIP: DocumentNode = gql`
  mutation DeleteAip($id: ID!) {
    deleteAip(id: $id)
  }
`;

// DIP Queries
export const GET_ALL_DIPS: DocumentNode = gql`
  query GetAllDips {
    getAllDips {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      sourceAipId
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        fields {
          id
          name
          label
          type
          value
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_DIPS_BY_TENANT: DocumentNode = gql`
  query GetDipsByTenant($tenantId: ID!) {
    getDipsByTenant(tenantId: $tenantId) {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      sourceAipId
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        fields {
          id
          name
          label
          type
          value
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_DIP: DocumentNode = gql`
  query GetDip($id: ID!) {
    getDip(id: $id) {
      id
      ownerId
      tenantId
      title
      description
      content
      createdAt
      updatedAt
      status
      standard
      sourceAipId
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        description
        fields {
          id
          name
          label
          type
          value
        }
        children {
          id
          elementIdentifier
          entityName
          entityType
          title
          description
          status
          createdAt
          createdBy
          fields {
            id
            name
            label
            type
            value
          }
          children {
            id
            elementIdentifier
            entityName
            entityType
            title
            status
            fields {
              id
              name
              label
              type
              value
            }
          }
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const CREATE_DIP: DocumentNode = gql`
  mutation CreateDip($input: CreateDipInput!) {
    createDip(input: $input) {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      sourceAipId
      rootElement {
        id
        elementIdentifier
        entityName
        entityType
        title
        fields {
          id
          name
          label
          type
          value
        }
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const DELETE_DIP: DocumentNode = gql`
  mutation DeleteDip($id: ID!) {
    deleteDip(id: $id)
  }
`;
