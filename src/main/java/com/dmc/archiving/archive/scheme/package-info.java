/**
 * Archive scheme entity management package.
 *
 * This package contains the domain model and services for managing archive standard
 * hierarchies (NOARK5 and OAIS). It provides:
 *
 * - Entity hierarchy definitions loaded from JSON
 * - Validation of parent-child relationships
 * - GraphQL API for querying scheme structures
 * - Tree-based representation of archive standards
 *
 * The scheme entities define what types of children each entity can contain,
 * enabling validation and UI guidance for archive structure creation.
 */
package com.dmc.archiving.archive.scheme;

