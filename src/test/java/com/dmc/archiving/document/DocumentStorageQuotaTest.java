package com.dmc.archiving.document;

import com.dmc.archiving.document.model.Document;
import com.dmc.archiving.document.repository.DocumentRepository;
import com.dmc.archiving.storage.CloudStorageService;
import com.dmc.archiving.storage.UploadResult;
import com.dmc.archiving.tenancy.api.TenancyApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies storage-allotment enforcement on the upload write-path (Risk 1b):
 * FREE hard-stops before the S3 write; paid plans allow billed overage;
 * within-limit, unlimited, and operator (non-billable) uploads proceed.
 */
class DocumentStorageQuotaTest {

    private static final Long TENANT = 100L;

    private final DocumentRepository repo = mock(DocumentRepository.class);
    private final CloudStorageService storage = mock(CloudStorageService.class);
    private final TenancyApi tenancyApi = mock(TenancyApi.class);
    private final DocumentService service = new DocumentService(repo, storage, tenancyApi);

    @BeforeEach
    void allowLargeFiles() {
        // This suite exercises storage-allotment logic, not the per-file cap.
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

    @Test
    void freePlanHardStopsBeforeWritingToStorage() throws Exception {
        when(tenancyApi.getStorageLimitBytes(TENANT)).thenReturn(1000L);
        when(tenancyApi.isOverageAllowed(TENANT)).thenReturn(false); // FREE
        when(repo.sumFileSizeByTenantId(TENANT)).thenReturn(900L);

        assertThatThrownBy(() ->
                service.uploadDocument(fileOf(200L), 2L, TENANT, "t", "d", true))
                .isInstanceOf(StorageQuotaExceededException.class)
                .hasMessageContaining("Storage limit exceeded");

        // Rejected before any S3 write.
        verify(storage, never()).uploadFile(any(), anyLong());
        verify(repo, never()).save(any());
    }

    @Test
    void paidPlanAllowsBilledOverage() throws Exception {
        when(tenancyApi.getStorageLimitBytes(TENANT)).thenReturn(1000L);
        when(tenancyApi.isOverageAllowed(TENANT)).thenReturn(true); // paid
        when(tenancyApi.getStorageOverageLimitBytes(TENANT)).thenReturn(1_000_000L); // ample cap
        when(repo.sumFileSizeByTenantId(TENANT)).thenReturn(900L);
        stubSuccessfulUpload();

        Document d = service.uploadDocument(fileOf(200L), 2L, TENANT, "t", "d", true);

        assertThat(d).isNotNull();
        verify(storage).uploadFile(any(), anyLong());
    }

    @Test
    void withinAllotmentProceeds() throws Exception {
        when(tenancyApi.getStorageLimitBytes(TENANT)).thenReturn(1000L);
        when(repo.sumFileSizeByTenantId(TENANT)).thenReturn(100L);
        stubSuccessfulUpload();

        service.uploadDocument(fileOf(200L), 2L, TENANT, "t", "d", true);

        verify(storage).uploadFile(any(), anyLong());
    }

    @Test
    void unlimitedPlanSkipsTheCheck() throws Exception {
        when(tenancyApi.getStorageLimitBytes(TENANT)).thenReturn(-1L); // ENTERPRISE
        stubSuccessfulUpload();

        service.uploadDocument(fileOf(9_999_999L), 2L, TENANT, "t", "d", true);

        verify(storage).uploadFile(any(), anyLong());
        verify(repo, never()).sumFileSizeByTenantId(anyLong()); // no usage query needed
    }

    @Test
    void operatorUploadBypassesQuota() throws Exception {
        // billable = false (ADMIN) must not be quota-limited and must not even
        // consult the limit.
        stubSuccessfulUpload();

        service.uploadDocument(fileOf(9_999_999L), 1L, TENANT, "t", "d", false);

        verify(storage).uploadFile(any(), anyLong());
        verify(tenancyApi, never()).getStorageLimitBytes(anyLong());
    }
}
