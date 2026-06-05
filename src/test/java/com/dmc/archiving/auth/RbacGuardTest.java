package com.dmc.archiving.auth;

import com.dmc.archiving.archive.ArchiveController;
import com.dmc.archiving.archive.ArchiveService;
import com.dmc.archiving.archive.input.CreateArchiveInput;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.strategy.ArchiveStrategyFactory;
import com.dmc.archiving.auth.api.AccessDeniedException;
import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.auth.api.AuthGuard;
import com.dmc.archiving.tenancy.api.BillingTenantResolver;
import com.dmc.archiving.tenancy.api.TenancyApi;
import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the RBAC boundary wired into GraphQL mutation handlers (Risk 1a):
 * a USER must be rejected from TENANT-scoped mutations, before the service is
 * ever touched, while a TENANT (or ADMIN) passes through.
 */
class RbacGuardTest {

    private DataFetchingEnvironment envWithRole(String role) {
        AuthContext ctx = role == null
                ? AuthContext.ANONYMOUS
                : new AuthContext(42L, role, role.toLowerCase());
        GraphQLContext gqlCtx = GraphQLContext.newContext().of("authContext", ctx).build();
        DataFetchingEnvironment env = mock(DataFetchingEnvironment.class);
        when(env.getGraphQlContext()).thenReturn(gqlCtx);
        return env;
    }

    // ---- AuthGuard unit coverage ----

    @Test
    void requireRole_rejectsUserFromTenantOnlyOperation() {
        assertThatThrownBy(() -> AuthGuard.requireRole(envWithRole("USER"), "TENANT", "ADMIN"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void requireRole_allowsTenantAndAdmin() {
        AuthGuard.requireRole(envWithRole("TENANT"), "TENANT", "ADMIN");
        AuthGuard.requireRole(envWithRole("ADMIN"), "TENANT", "ADMIN");
        // no exception == pass
    }

    @Test
    void requireRole_rejectsAnonymous() {
        assertThatThrownBy(() -> AuthGuard.requireRole(envWithRole(null), "TENANT", "ADMIN"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Authentication required");
    }

    // ---- Controller boundary: the guard fires before the service ----

    @Test
    void createArchive_deniesUser_andNeverCallsService() {
        ArchiveService service = mock(ArchiveService.class);
        ArchiveController controller = new ArchiveController(
                service, mock(ArchiveStrategyFactory.class), mock(TenancyApi.class),
                mock(BillingTenantResolver.class));

        assertThatThrownBy(() ->
                controller.createArchive(new CreateArchiveInput(), envWithRole("USER")))
                .isInstanceOf(AccessDeniedException.class);

        verify(service, never()).createArchive(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createArchive_allowsTenant_andReachesService() {
        ArchiveService service = mock(ArchiveService.class);
        CreateArchiveInput input = new CreateArchiveInput();
        Archive expected = new Archive();
        when(service.createArchive(input)).thenReturn(expected);

        BillingTenantResolver resolver = mock(BillingTenantResolver.class);
        when(resolver.resolve(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(100L);

        ArchiveController controller = new ArchiveController(
                service, mock(ArchiveStrategyFactory.class), mock(TenancyApi.class), resolver);

        Archive result = controller.createArchive(input, envWithRole("TENANT"));

        assertThat(result).isSameAs(expected);
        verify(service).createArchive(input);
    }
}
