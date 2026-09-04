package com.dmc.archiving.release;

import com.dmc.archiving.release.generator.ReleaseGeneratorFactory;
import com.dmc.archiving.release.input.CreateReleaseInput;
import com.dmc.archiving.release.model.Release;
import com.dmc.archiving.release.repository.ReleaseRepository;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.tenancy.api.OverageSpendCapException;
import com.dmc.archiving.tenancy.api.PremiumOverageGuard;
import com.dmc.archiving.tenancy.api.PremiumPackageType;
import com.dmc.archiving.user.api.UserApi;
import com.dmc.archiving.user.model.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the premium spend-cap check runs INSIDE createRelease's transaction
 * (Review H5/M9 — the DIP side was previously untested): a billable premium
 * create is gated by the guard (and blocked before the insert when over cap);
 * non-premium and operator (non-billable) creates skip the guard; a billable
 * premium create RECORDS a ledger event while the others record nothing; and a
 * null tenantId is rejected rather than billed to the owner.
 */
class ReleaseServicePremiumCapTest {

    private static final Long TENANT = 100L;

    private final ReleaseRepository repo = mock(ReleaseRepository.class);
    private final UserApi userApi = mock(UserApi.class);
    private final PremiumOverageGuard guard = mock(PremiumOverageGuard.class);
    private final ReleaseService service =
            new ReleaseService(repo, userApi, mock(ReleaseGeneratorFactory.class), guard);

    private CreateReleaseInput input(ArchiveStandard standard, boolean billable) {
        CreateReleaseInput in = new CreateReleaseInput();
        in.setUserId(2L);
        in.setTenantId(TENANT);
        in.setTitle("pkg");
        in.setStandard(standard);
        in.setBillable(billable);
        return in;
    }

    private void stubUserAndSave() {
        when(userApi.getUserById(2L)).thenReturn(Optional.of(new User()));
        when(repo.save(any(Release.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void billablePremiumOverCapIsBlockedBeforeInsert() {
        when(userApi.getUserById(2L)).thenReturn(Optional.of(new User())); // user lookup precedes the guard
        when(guard.isPremiumStandard("NOARK5")).thenReturn(true);
        doThrow(new OverageSpendCapException("over cap"))
                .when(guard).checkCanCreatePremiumPackage(TENANT);

        assertThatThrownBy(() -> service.createRelease(input(ArchiveStandard.NOARK5, true)))
                .isInstanceOf(OverageSpendCapException.class);

        verify(guard).checkCanCreatePremiumPackage(TENANT);
        verify(repo, never()).save(any());
        verify(guard, never()).recordPremiumPackageGenerated(anyLong(), any(), any()); // blocked → no ledger event
    }

    @Test
    void billablePremiumWithinCapIsSavedAndRecorded() {
        when(guard.isPremiumStandard("NOARK5")).thenReturn(true); // guard allows (no throw)
        stubUserAndSave();

        assertThatCode(() -> service.createRelease(input(ArchiveStandard.NOARK5, true)))
                .doesNotThrowAnyException();

        verify(guard).checkCanCreatePremiumPackage(TENANT);
        verify(repo).save(any(Release.class));
        // The generation is recorded to the append-only ledger after the save.
        verify(guard).recordPremiumPackageGenerated(TENANT, "NOARK5", PremiumPackageType.RELEASE);
    }

    @Test
    void nonPremiumStandardSkipsTheGuard() {
        when(guard.isPremiumStandard("OAIS")).thenReturn(false);
        stubUserAndSave();

        service.createRelease(input(ArchiveStandard.OAIS, true));

        verify(guard, never()).checkCanCreatePremiumPackage(anyLong());
        verify(guard, never()).recordPremiumPackageGenerated(anyLong(), any(), any());
        verify(repo).save(any(Release.class));
    }

    @Test
    void operatorCreatedPremiumSkipsTheGuard() {
        stubUserAndSave();

        service.createRelease(input(ArchiveStandard.NOARK5, false)); // billable=false (ADMIN)

        verify(guard, never()).checkCanCreatePremiumPackage(anyLong());
        verify(guard, never()).recordPremiumPackageGenerated(anyLong(), any(), any()); // operator not metered
        verify(repo).save(any(Release.class));
    }

    @Test
    void nullTenantIdIsRejected_neverBilledToTheOwner() {
        when(userApi.getUserById(2L)).thenReturn(Optional.of(new User()));
        CreateReleaseInput in = input(ArchiveStandard.NOARK5, true);
        in.setTenantId(null); // no resolved billing tenant — must fail, not fall back to ownerId

        assertThatThrownBy(() -> service.createRelease(in))
                .isInstanceOf(IllegalArgumentException.class);

        verify(guard, never()).checkCanCreatePremiumPackage(anyLong());
        verify(guard, never()).recordPremiumPackageGenerated(anyLong(), any(), any());
        verify(repo, never()).save(any());
    }
}
