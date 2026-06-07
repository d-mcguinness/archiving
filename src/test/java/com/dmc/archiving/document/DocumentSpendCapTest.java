package com.dmc.archiving.document;

import com.dmc.archiving.document.model.Document;
import com.dmc.archiving.document.repository.DocumentRepository;
import com.dmc.archiving.storage.CloudStorageService;
import com.dmc.archiving.storage.UploadResult;
import com.dmc.archiving.tenancy.api.TenancyApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the soft spend cap on the storage overage rail (Risk 3a): a paid
 * tenant past their overage cap is blocked (stops accruing) unless they have
 * opted in; an unlimited cap keeps accruing.
 */
class DocumentSpendCapTest {

    private static final Long TENANT = 100L;

    private final DocumentRepository repo = mock(DocumentRepository.class);
    private final CloudStorageService storage = mock(CloudStorageService.class);
    private final TenancyApi tenancyApi = mock(TenancyApi.class);
    private final DocumentService service = new DocumentService(repo, storage, tenancyApi);

    @BeforeEach
    void allowLargeFiles() {
        // This suite exercises the spend cap, not the per-file size cap.
        when(tenancyApi.getMaxUploadFileSizeBytes(anyLong())).thenReturn(Long.MAX_VALUE);
    }

    private static MultipartFile fileOf(long size) {
        MultipartFile f = mock(MultipartFile.class);
        when(f.getSize()).thenReturn(size);
        return f;
    }

    private void stubSuccessfulUpload() throws Exception {
        when(storage.uploadFile(any(), anyLong())).thenReturn(UploadResult.builder()
                .fileKey("k").fileUrl("u").originalFilename("f")
                .contentType("application/pdf").fileSize(100L).build());
        when(repo.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // Paid tenant, 1000-byte allotment, already at 1000; an incoming 500 bytes
    // is 500 of overage.
    private void paidAtAllotment() {
        when(tenancyApi.getStorageLimitBytes(TENANT)).thenReturn(1000L);
        when(tenancyApi.isOverageAllowed(TENANT)).thenReturn(true);
        when(repo.sumFileSizeByTenantId(TENANT)).thenReturn(1000L);
    }

    @Test
    void blocksWhenProjectedOverageExceedsCapAndNotOptedIn() throws Exception {
        paidAtAllotment();
        when(tenancyApi.getStorageOverageLimitBytes(TENANT)).thenReturn(200L); // cap < 500 overage
        when(tenancyApi.isOverageOptIn(TENANT)).thenReturn(false);

        assertThatThrownBy(() ->
                service.uploadDocument(fileOf(500L), 2L, TENANT, "t", "d", true))
                .isInstanceOf(SpendCapExceededException.class)
                .hasMessageContaining("Overage spend cap reached");

        verify(storage, never()).uploadFile(any(), anyLong());
        verify(repo, never()).save(any());
    }

    @Test
    void allowsPastCapWhenOptedIn() throws Exception {
        paidAtAllotment();
        when(tenancyApi.getStorageOverageLimitBytes(TENANT)).thenReturn(200L); // over cap...
        when(tenancyApi.isOverageOptIn(TENANT)).thenReturn(true);              // ...but opted in
        stubSuccessfulUpload();

        service.uploadDocument(fileOf(500L), 2L, TENANT, "t", "d", true);

        verify(storage).uploadFile(any(), anyLong());
    }

    @Test
    void allowsOverageWithinCap() throws Exception {
        paidAtAllotment();
        when(tenancyApi.getStorageOverageLimitBytes(TENANT)).thenReturn(1000L); // cap >= 500 overage
        stubSuccessfulUpload();

        service.uploadDocument(fileOf(500L), 2L, TENANT, "t", "d", true);

        verify(storage).uploadFile(any(), anyLong());
    }

    @Test
    void unlimitedOverageKeepsAccruing() throws Exception {
        paidAtAllotment();
        when(tenancyApi.getStorageOverageLimitBytes(TENANT)).thenReturn(-1L); // unlimited
        stubSuccessfulUpload();

        service.uploadDocument(fileOf(9_999_999L), 2L, TENANT, "t", "d", true);

        verify(storage).uploadFile(any(), anyLong());
        // opt-in is irrelevant when overage is unlimited
        verify(tenancyApi, never()).isOverageOptIn(anyLong());
    }
}
