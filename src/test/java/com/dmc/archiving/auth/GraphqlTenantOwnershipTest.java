package com.dmc.archiving.auth;

import com.dmc.archiving.aip.AipController;
import com.dmc.archiving.aip.AipService;
import com.dmc.archiving.aip.model.Aip;
import com.dmc.archiving.aip.model.AipStatus;
import com.dmc.archiving.auth.api.AccessDeniedException;
import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.common.exception.ResourceNotFoundException;
import com.dmc.archiving.pkg.PackageController;
import com.dmc.archiving.pkg.PackageService;
import com.dmc.archiving.pkg.model.ArchivalPackage;
import com.dmc.archiving.tenancy.api.BillingTenantResolver;
import com.dmc.archiving.tenancy.api.TenancyApi;
import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies by-id GraphQL mutations enforce tenant ownership (Review H1): a
 * TENANT may only act on resources in a tenant they belong to; ADMIN bypasses;
 * a foreign-tenant TENANT is denied before the service runs. Covers the base
 * pattern (AIP via requireTenantAccess) and the non-base pattern (Package).
 */
class GraphqlTenantOwnershipTest {

    private static final Long ID = 7L;
    private static final Long OWNING_TENANT = 100L;

    private static DataFetchingEnvironment env(Long userId, String role) {
        AuthContext ctx = new AuthContext(userId, role, role.toLowerCase());
        GraphQLContext gql = GraphQLContext.newContext().of("authContext", ctx).build();
        DataFetchingEnvironment e = mock(DataFetchingEnvironment.class);
        when(e.getGraphQlContext()).thenReturn(gql);
        return e;
    }

    // ---- AIP (base controller: requireTenantAccess) ----

    private AipController aipController(AipService svc, TenancyApi tenancy) {
        return new AipController(svc, tenancy, mock(BillingTenantResolver.class));
    }

    private static Aip aipInTenant() {
        Aip a = new Aip();
        a.setId(ID);
        a.setTenantId(OWNING_TENANT);
        return a;
    }

    @Test
    void deleteAip_foreignTenantTenantIsDenied_serviceNotCalled() {
        AipService svc = mock(AipService.class);
        TenancyApi tenancy = mock(TenancyApi.class);
        when(svc.getAip(ID)).thenReturn(aipInTenant());
        when(tenancy.isUserInTenant(2L, OWNING_TENANT)).thenReturn(false);

        assertThatThrownBy(() -> aipController(svc, tenancy).deleteAip(ID, env(2L, "TENANT")))
                .isInstanceOf(AccessDeniedException.class);

        verify(svc, never()).deleteAip(anyLong());
    }

    @Test
    void deleteAip_memberTenantProceeds() {
        AipService svc = mock(AipService.class);
        TenancyApi tenancy = mock(TenancyApi.class);
        when(svc.getAip(ID)).thenReturn(aipInTenant());
        when(tenancy.isUserInTenant(2L, OWNING_TENANT)).thenReturn(true);
        when(svc.deleteAip(ID)).thenReturn(true);

        aipController(svc, tenancy).deleteAip(ID, env(2L, "TENANT"));

        verify(svc).deleteAip(ID);
    }

    @Test
    void updateAipStatus_adminBypassesMembership() {
        AipService svc = mock(AipService.class);
        TenancyApi tenancy = mock(TenancyApi.class);
        when(svc.getAip(ID)).thenReturn(aipInTenant());

        aipController(svc, tenancy).updateAipStatus(ID, AipStatus.STORED, env(1L, "ADMIN"));

        verify(svc).updateAipStatus(ID, AipStatus.STORED);
        verify(tenancy, never()).isUserInTenant(anyLong(), anyLong());
    }

    @Test
    void generateAip_missingAipIsNotFound() {
        AipService svc = mock(AipService.class);
        TenancyApi tenancy = mock(TenancyApi.class);
        when(svc.getAip(ID)).thenReturn(null);

        assertThatThrownBy(() -> aipController(svc, tenancy).generateAip(ID, env(1L, "ADMIN")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(svc, never()).generateAip(anyLong());
    }

    // ---- Package (non-base controller: AuthGuard.context + TenancyApi) ----

    private static ArchivalPackage pkgInTenant() {
        ArchivalPackage p = new ArchivalPackage();
        p.setId(ID);
        p.setTenantId(OWNING_TENANT);
        return p;
    }

    @Test
    void deletePackage_foreignTenantTenantIsDenied_serviceNotCalled() {
        PackageService svc = mock(PackageService.class);
        TenancyApi tenancy = mock(TenancyApi.class);
        when(svc.getPackage(ID)).thenReturn(pkgInTenant());
        when(tenancy.isUserInTenant(2L, OWNING_TENANT)).thenReturn(false);
        PackageController controller = new PackageController(svc, mock(BillingTenantResolver.class), tenancy);

        assertThatThrownBy(() -> controller.deletePackage(ID.toString(), env(2L, "TENANT")))
                .isInstanceOf(AccessDeniedException.class);

        verify(svc, never()).deletePackage(anyLong());
    }

    @Test
    void deletePackage_adminProceeds() {
        PackageService svc = mock(PackageService.class);
        TenancyApi tenancy = mock(TenancyApi.class);
        when(svc.getPackage(ID)).thenReturn(pkgInTenant());
        when(svc.deletePackage(ID)).thenReturn(true);
        PackageController controller = new PackageController(svc, mock(BillingTenantResolver.class), tenancy);

        assertThatCode(() -> controller.deletePackage(ID.toString(), env(1L, "ADMIN")))
                .doesNotThrowAnyException();

        verify(svc).deletePackage(ID);
    }

    // ---- Element (base controller: tenant via owning archive) ----

    @Test
    void deleteElement_foreignTenantTenantIsDenied_serviceNotCalled() {
        com.dmc.archiving.archive.element.ElementService elementService =
                mock(com.dmc.archiving.archive.element.ElementService.class);
        TenancyApi tenancy = mock(TenancyApi.class);
        when(elementService.getArchiveTenantId(ID)).thenReturn(OWNING_TENANT);
        when(tenancy.isUserInTenant(2L, OWNING_TENANT)).thenReturn(false);
        var controller = new com.dmc.archiving.archive.element.ElementController(
                elementService, mock(com.dmc.archiving.archive.repository.ArchiveRepository.class), tenancy);

        assertThatThrownBy(() -> controller.deleteElement(ID, env(2L, "TENANT")))
                .isInstanceOf(AccessDeniedException.class);

        verify(elementService, never()).deleteElement(anyLong());
    }

    // ---- ElementLink (non-base controller: tenant via the link's element) ----

    @Test
    void deleteElementLink_foreignTenantTenantIsDenied_serviceNotCalled() {
        var linkService = mock(com.dmc.archiving.archive.element.link.ElementLinkService.class);
        var elementService = mock(com.dmc.archiving.archive.element.ElementService.class);
        TenancyApi tenancy = mock(TenancyApi.class);

        com.dmc.archiving.archive.element.Element source =
                mock(com.dmc.archiving.archive.element.Element.class);
        when(source.getId()).thenReturn(50L);
        var link = mock(com.dmc.archiving.archive.element.link.ElementLink.class);
        when(link.getSourceElement()).thenReturn(source);
        when(linkService.getLink(ID)).thenReturn(link);
        when(elementService.getArchiveTenantId(50L)).thenReturn(OWNING_TENANT);
        when(tenancy.isUserInTenant(2L, OWNING_TENANT)).thenReturn(false);

        var controller = new com.dmc.archiving.archive.element.link.ElementLinkController(
                linkService, elementService, tenancy);

        assertThatThrownBy(() -> controller.deleteElementLink(ID.toString(), env(2L, "TENANT")))
                .isInstanceOf(AccessDeniedException.class);

        verify(linkService, never()).deleteLink(anyLong());
    }
}
