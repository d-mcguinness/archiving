package com.dmc.archiving.tenancy;

import com.dmc.archiving.auth.api.AccessDeniedException;
import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.tenancy.service.TenancyService;
import graphql.GraphQLContext;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the tenant-membership mutations no longer swallow domain failures to
 * a bland {@code false} (Review H7/L3/L7): they return true on success, enforce
 * the ADMIN role, and let the cause propagate so GlobalExceptionHandler can turn
 * it into a classified, message-bearing GraphQL error.
 */
class GraphqlTenancyControllerTest {

    private final TenancyService service = mock(TenancyService.class);
    private final GraphqlTenancyController controller = new GraphqlTenancyController(service);

    private static DataFetchingEnvironment env(String role) {
        AuthContext ctx = new AuthContext(1L, role, role.toLowerCase());
        GraphQLContext gql = GraphQLContext.newContext().of("authContext", ctx).build();
        DataFetchingEnvironment e = mock(DataFetchingEnvironment.class);
        when(e.getGraphQlContext()).thenReturn(gql);
        return e;
    }

    @Test
    void addUserToTenant_success_returnsTrue() {
        assertThat(controller.addUserToTenant(3L, 100L, env("ADMIN"))).isTrue();
        verify(service).addUserToTenant(3L, 100L);
    }

    @Test
    void addUserToTenant_seatLimitPropagates_notSwallowedToFalse() {
        doThrow(new IllegalStateException("Seat limit reached for tenant 100"))
                .when(service).addUserToTenant(3L, 100L);

        assertThatThrownBy(() -> controller.addUserToTenant(3L, 100L, env("ADMIN")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Seat limit reached");
    }

    @Test
    void removeUserFromTenant_unknownTenantPropagates_notSwallowedToFalse() {
        doThrow(new IllegalArgumentException("Tenant with ID 999 does not exist"))
                .when(service).removeUserFromTenant(999L, 3L);

        assertThatThrownBy(() -> controller.removeUserFromTenant(999L, 3L, env("ADMIN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void addUserToTenant_nonAdminIsDenied_serviceNotCalled() {
        assertThatThrownBy(() -> controller.addUserToTenant(3L, 100L, env("USER")))
                .isInstanceOf(AccessDeniedException.class);

        verify(service, never()).addUserToTenant(anyLong(), anyLong());
    }
}
