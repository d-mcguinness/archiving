package com.dmc.archiving.archive;

import com.dmc.archiving.archive.element.ElementRepository;
import com.dmc.archiving.archive.input.CreateArchiveInput;
import com.dmc.archiving.archive.model.Archive;
import com.dmc.archiving.archive.model.ArchiveStandard;
import com.dmc.archiving.archive.repository.ArchiveRepository;
import com.dmc.archiving.tenancy.api.TenancyApi;
import com.dmc.archiving.user.api.UserApi;
import com.dmc.archiving.user.model.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies maxArchives enforcement at create time (Risk 1c): FREE hard-stops at
 * the allotment; paid plans allow billed overage; unlimited/within proceed.
 */
class ArchiveQuotaTest {

    private static final Long TENANT = 100L;

    private final ArchiveRepository repo = mock(ArchiveRepository.class);
    private final UserApi userApi = mock(UserApi.class);
    private final TenancyApi tenancyApi = mock(TenancyApi.class);
    private final ArchiveService service =
            new ArchiveService(repo, userApi, mock(ElementRepository.class), tenancyApi);

    private CreateArchiveInput input() {
        CreateArchiveInput in = new CreateArchiveInput();
        in.setUserId(2L);
        in.setTenantId(TENANT);
        in.setTitle("Records");
        in.setStandard(ArchiveStandard.NOARK5);
        return in;
    }

    private void stubUserAndSave() {
        when(userApi.getUserById(2L)).thenReturn(Optional.of(new User()));
        lenient().when(repo.save(any(Archive.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void freePlanHardStopsAtArchiveLimit() {
        stubUserAndSave();
        when(tenancyApi.getArchiveLimit(TENANT)).thenReturn(10);
        when(repo.countByTenantId(TENANT)).thenReturn(10L);
        when(tenancyApi.isOverageAllowed(TENANT)).thenReturn(false); // FREE

        assertThatThrownBy(() -> service.createArchive(input()))
                .isInstanceOf(ArchiveQuotaExceededException.class)
                .hasMessageContaining("Archive limit reached");

        verify(repo, never()).save(any());
    }

    @Test
    void paidPlanAllowsBilledOverage() {
        stubUserAndSave();
        when(tenancyApi.getArchiveLimit(TENANT)).thenReturn(10);
        when(repo.countByTenantId(TENANT)).thenReturn(10L);
        when(tenancyApi.isOverageAllowed(TENANT)).thenReturn(true); // paid

        service.createArchive(input());

        verify(repo).save(any(Archive.class));
    }

    @Test
    void withinAllotmentProceeds() {
        stubUserAndSave();
        when(tenancyApi.getArchiveLimit(TENANT)).thenReturn(10);
        when(repo.countByTenantId(TENANT)).thenReturn(3L);

        service.createArchive(input());

        verify(repo).save(any(Archive.class));
    }

    @Test
    void unlimitedPlanSkipsTheCheck() {
        stubUserAndSave();
        when(tenancyApi.getArchiveLimit(TENANT)).thenReturn(-1);

        service.createArchive(input());

        verify(repo).save(any(Archive.class));
        verify(repo, never()).countByTenantId(anyLong());
    }
}
