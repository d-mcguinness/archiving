package com.dmc.archiving.archive;

import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.archive.strategy.ArchiveStrategy;
import com.dmc.archiving.archive.strategy.ArchiveStrategyFactory;
import com.dmc.archiving.archive.strategy.ValidationResult;
import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.tenancy.api.BillingTenantResolver;
import com.dmc.archiving.tenancy.api.TenancyApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Who may call POST /api/archives/{id}/validate. */
class ArchiveRestAuthTest {

    private static final AuthContext MEMBER = new AuthContext(5L, "USER", "member");
    private static final AuthContext ADMIN = new AuthContext(1L, "ADMIN", "admin");

    private final ArchiveService service = mock(ArchiveService.class);
    private final ArchiveStrategyFactory strategies = mock(ArchiveStrategyFactory.class);
    private final ArchiveStrategy strategy = mock(ArchiveStrategy.class);
    private final TenancyApi tenancy = mock(TenancyApi.class);
    private final ArchiveController controller = new ArchiveController(
            service, strategies, tenancy, mock(BillingTenantResolver.class));

    private static MockHttpServletRequest as(AuthContext ctx) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        if (ctx != null) r.setAttribute(ArchiveController.REST_AUTH_CONTEXT, ctx);
        return r;
    }

    @BeforeEach
    void archive7BelongsToTenant100() {
        Archive archive = new Archive();
        archive.setId(7L);
        archive.setTenantId(100L);
        archive.setStandard(ArchiveStandard.EARK);
        when(service.getArchiveById(7L)).thenReturn(archive);
        when(strategies.getStrategy(ArchiveStandard.EARK)).thenReturn(strategy);
        when(strategy.getStandardName()).thenReturn("EARK");
        when(strategy.validate(archive)).thenReturn(new ValidationResult());
    }

    // ---- validate ----

    @Test
    void validate_anonymousIs401BeforeAnyWork() {
        ResponseEntity<?> r = controller.validateArchive(7L, as(null));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(service, strategies);
    }

    @Test
    void validate_memberOfAnotherTenantIs403() {
        when(tenancy.isUserInTenant(5L, 100L)).thenReturn(false);
        ResponseEntity<?> r = controller.validateArchive(7L, as(MEMBER));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(strategy, never()).validate(any());
    }

    @Test
    void validate_tenantMemberIs200() {
        when(tenancy.isUserInTenant(5L, 100L)).thenReturn(true);
        assertThat(controller.validateArchive(7L, as(MEMBER)).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void validate_adminIs200() {
        assertThat(controller.validateArchive(7L, as(ADMIN)).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(tenancy, never()).isUserInTenant(anyLong(), anyLong());
    }
}
