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
  mutation RemoveUserFromTenant($tenantId: ID!, $userId: ID!) {
    removeUserFromTenant(tenantId: $tenantId, userId: $userId)
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

export const GET_ALL_INTAKES: DocumentNode = gql`
  query GetAllIntakes {
    getAllIntakes {
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

export const GET_INTAKES_BY_TENANT: DocumentNode = gql`
  query GetIntakesByTenant($tenantId: ID!) {
    getIntakesByTenant(tenantId: $tenantId) {
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

export const PREFILL_INTAKE_FIELDS: DocumentNode = gql`
  query PrefillIntakeFields($standard: ArchiveStandard!, $fileMetadata: FileMetadataInput!) {
    prefillIntakeFields(standard: $standard, fileMetadata: $fileMetadata) {
      name
      value
    }
  }
`;

// Intake V2 Queries (dedicated Intake entity)
export const GET_ALL_INTAKES_V2: DocumentNode = gql`
  query GetAllIntakesV2 {
    getAllIntakesV2 {
      id
      ownerId
      tenantId
      archiveId
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

export const GET_INTAKES_BY_TENANT_V2: DocumentNode = gql`
  query GetIntakesByTenantV2($tenantId: ID!) {
    getIntakesByTenantV2(tenantId: $tenantId) {
      id
      ownerId
      tenantId
      archiveId
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

export const GET_INTAKE: DocumentNode = gql`
  query GetIntake($id: ID!) {
    getIntake(id: $id) {
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
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const CREATE_INTAKE: DocumentNode = gql`
  mutation CreateIntakeV2($input: CreateIntakeInput!) {
    createIntakeV2(input: $input) {
      id
      ownerId
      tenantId
      archiveId
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

export const DELETE_INTAKE: DocumentNode = gql`
  mutation DeleteIntakeV2($id: ID!) {
    deleteIntakeV2(id: $id)
  }
`;

// Preservation Queries
export const GET_ALL_PRESERVATIONS: DocumentNode = gql`
  query GetAllPreservations {
    getAllPreservations {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      sourceIntakeId
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

export const GET_PRESERVATIONS_BY_TENANT: DocumentNode = gql`
  query GetPreservationsByTenant($tenantId: ID!) {
    getPreservationsByTenant(tenantId: $tenantId) {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      sourceIntakeId
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

export const GET_PRESERVATION: DocumentNode = gql`
  query GetPreservation($id: ID!) {
    getPreservation(id: $id) {
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
      sourceIntakeId
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

export const CREATE_PRESERVATION: DocumentNode = gql`
  mutation CreatePreservation($input: CreatePreservationInput!) {
    createPreservation(input: $input) {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      sourceIntakeId
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

export const DELETE_PRESERVATION: DocumentNode = gql`
  mutation DeletePreservation($id: ID!) {
    deletePreservation(id: $id)
  }
`;

// Release Queries
export const GET_ALL_RELEASES: DocumentNode = gql`
  query GetAllReleases {
    getAllReleases {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      sourcePreservationId
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

export const GET_RELEASES_BY_TENANT: DocumentNode = gql`
  query GetReleasesByTenant($tenantId: ID!) {
    getReleasesByTenant(tenantId: $tenantId) {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      sourcePreservationId
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

export const GET_RELEASE: DocumentNode = gql`
  query GetRelease($id: ID!) {
    getRelease(id: $id) {
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
      sourcePreservationId
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

export const CREATE_RELEASE: DocumentNode = gql`
  mutation CreateRelease($input: CreateReleaseInput!) {
    createRelease(input: $input) {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      status
      standard
      sourcePreservationId
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

export const DELETE_RELEASE: DocumentNode = gql`
  mutation DeleteRelease($id: ID!) {
    deleteRelease(id: $id)
  }
`;

// ═══════════════════════════════════════════════
// Element Link Queries
// ═══════════════════════════════════════════════

export const GET_ELEMENT_LINKS: DocumentNode = gql`
  query GetElementLinks($elementId: ID!) {
    getElementLinks(elementId: $elementId) {
      id
      sourceElement { id elementIdentifier entityName title }
      targetElement { id elementIdentifier entityName title }
      linkType
      label
      description
      directional
      createdAt
      createdBy
    }
  }
`;

export const CREATE_ELEMENT_LINK: DocumentNode = gql`
  mutation CreateElementLink($input: CreateElementLinkInput!) {
    createElementLink(input: $input) {
      id
      linkType
      label
      sourceElement { id elementIdentifier entityName title }
      targetElement { id elementIdentifier entityName title }
    }
  }
`;

export const DELETE_ELEMENT_LINK: DocumentNode = gql`
  mutation DeleteElementLink($id: ID!) {
    deleteElementLink(id: $id)
  }
`;

// ═══════════════════════════════════════════════
// Unified Package Queries (Intake/Preservation/Release)
// ═══════════════════════════════════════════════

export const GET_ALL_PACKAGES: DocumentNode = gql`
  query GetAllPackages($stage: PackageStage) {
    getAllPackages(stage: $stage) {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      stage
      status
      standard
      sourceArchiveId
      sourcePackageId
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
        children {
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
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const GET_PACKAGES_BY_TENANT: DocumentNode = gql`
  query GetPackagesByTenant($tenantId: ID!, $stage: PackageStage) {
    getPackagesByTenant(tenantId: $tenantId, stage: $stage) {
      id
      ownerId
      tenantId
      title
      description
      createdAt
      updatedAt
      stage
      status
      standard
      sourceArchiveId
      sourcePackageId
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

export const GET_PACKAGE: DocumentNode = gql`
  query GetPackage($id: ID!) {
    getPackage(id: $id) {
      id
      ownerId
      tenantId
      title
      description
      content
      createdAt
      updatedAt
      stage
      status
      standard
      sourceArchiveId
      sourcePackageId
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
      }
      assignedUsers {
        id
        name
        email
      }
    }
  }
`;

export const CREATE_PACKAGE: DocumentNode = gql`
  mutation CreatePackage($input: CreatePackageInput!) {
    createPackage(input: $input) {
      id
      tenantId
      ownerId
      title
      stage
      status
      standard
      createdAt
      rootElement {
        id
        elementIdentifier
        entityName
      }
    }
  }
`;

export const UPDATE_PACKAGE_STATUS: DocumentNode = gql`
  mutation UpdatePackageStatus($packageId: ID!, $status: PackageStatus!) {
    updatePackageStatus(packageId: $packageId, status: $status) {
      id
      status
      updatedAt
    }
  }
`;

export const DELETE_PACKAGE: DocumentNode = gql`
  mutation DeletePackage($id: ID!) {
    deletePackage(id: $id)
  }
`;
