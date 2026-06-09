package com.dmc.archiving.dip;

import com.dmc.archiving.dip.generator.DipGeneratorFactory;
import com.dmc.archiving.dip.input.CreateDipInput;
import com.dmc.archiving.dip.model.Dip;
import com.dmc.archiving.dip.repository.DipRepository;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.tenancy.api.OverageSpendCapException;
import com.dmc.archiving.tenancy.api.PremiumOverageGuard;
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
 * Verifies the premium spend-cap check runs INSIDE createDip's transaction
 * (Review H5/M9 — the DIP side was previously untested): a billable premium
 * create is gated by the guard (and blocked before the insert when over cap);
 * non-premium and operator (non-billable) creates skip the guard; and a null
 * tenantId is rejected rather than billed to the owner.
 */
class DipServicePremiumCapTest {

    private static final Long TENANT = 100L;

    private final DipRepository repo = mock(DipRepository.class);
    private final UserApi userApi = mock(UserApi.class);
    private final PremiumOverageGuard guard = mock(PremiumOverageGuard.class);
    private final DipService service =
            new DipService(repo, userApi, mock(DipGeneratorFactory.class), guard);

    private CreateDipInput input(ArchiveStandard standard, boolean billable) {
        CreateDipInput in = new CreateDipInput();
        in.setUserId(2L);
        in.setTenantId(TENANT);
        in.setTitle("pkg");
        in.setStandard(standard);
        in.setBillable(billable);
        return in;
    }

    private void stubUserAndSave() {
        when(userApi.getUserById(2L)).thenReturn(Optional.of(new User()));
        when(repo.save(any(Dip.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void billablePremiumOverCapIsBlockedBeforeInsert() {
        when(userApi.getUserById(2L)).thenReturn(Optional.of(new User())); // user lookup precedes the guard
        when(guard.isPremiumStandard("NOARK5")).thenReturn(true);
        doThrow(new OverageSpendCapException("over cap"))
                .when(guard).checkCanCreatePremiumPackage(TENANT);

        assertThatThrownBy(() -> service.createDip(input(ArchiveStandard.NOARK5, true)))
                .isInstanceOf(OverageSpendCapException.class);

        verify(guard).checkCanCreatePremiumPackage(TENANT);
        verify(repo, never()).save(any());
    }

    @Test
    void billablePremiumWithinCapIsSaved() {
        when(guard.isPremiumStandard("NOARK5")).thenReturn(true); // guard allows (no throw)
        stubUserAndSave();

        assertThatCode(() -> service.createDip(input(ArchiveStandard.NOARK5, true)))
                .doesNotThrowAnyException();

        verify(guard).checkCanCreatePremiumPackage(TENANT);
        verify(repo).save(any(Dip.class));
    }

    @Test
    void nonPremiumStandardSkipsTheGuard() {
        when(guard.isPremiumStandard("OAIS")).thenReturn(false);
        stubUserAndSave();

        service.createDip(input(ArchiveStandard.OAIS, true));

        verify(guard, never()).checkCanCreatePremiumPackage(anyLong());
        verify(repo).save(any(Dip.class));
    }

    @Test
    void operatorCreatedPremiumSkipsTheGuard() {
        stubUserAndSave();

        service.createDip(input(ArchiveStandard.NOARK5, false)); // billable=false (ADMIN)

        verify(guard, never()).checkCanCreatePremiumPackage(anyLong());
        verify(repo).save(any(Dip.class));
    }

    @Test
    void nullTenantIdIsRejected_neverBilledToTheOwner() {
        when(userApi.getUserById(2L)).thenReturn(Optional.of(new User()));
        CreateDipInput in = input(ArchiveStandard.NOARK5, true);
        in.setTenantId(null); // no resolved billing tenant — must fail, not fall back to ownerId

        assertThatThrownBy(() -> service.createDip(in))
                .isInstanceOf(IllegalArgumentException.class);

        verify(guard, never()).checkCanCreatePremiumPackage(anyLong());
        verify(repo, never()).save(any());
    }
}
