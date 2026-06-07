package com.dmc.archiving.document;

import com.dmc.archiving.document.model.Document;
import com.dmc.archiving.document.repository.DocumentRepository;
import com.dmc.archiving.storage.CloudStorageService;
import com.dmc.archiving.storage.UploadResult;
import com.dmc.archiving.tenancy.api.TenancyApi;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the per-plan per-file size cap (Risk 3c): standard plans reject
 * files over the default ceiling; the raised ENTERPRISE/CUSTOM ceiling lets
 * large preservation masters through (the streaming putObject path supports
 * them up to the 5GB single-PUT limit).
 */
class DocumentFileSizeLimitTest {

    private static final Long TENANT = 100L;
    private static final long MB = 1024L * 1024;

    private final DocumentRepository repo = mock(DocumentRepository.class);
    private final CloudStorageService storage = mock(CloudStorageService.class);
    private final TenancyApi tenancyApi = mock(TenancyApi.class);
    private final DocumentService service = new DocumentService(repo, storage, tenancyApi);

    private static MultipartFile fileOf(long size) {
        MultipartFile f = mock(MultipartFile.class);
        when(f.getSize()).thenReturn(size);
        return f;
    }

    private void stubSuccessfulUpload() throws Exception {
        when(storage.uploadFile(any(), anyLong())).thenReturn(UploadResult.builder()
                .fileKey("k").fileUrl("u").originalFilename("f")
                .contentType("application/octet-stream").fileSize(1L).build());
        when(repo.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));
        // ample storage allotment so only the file-size cap is under test
        when(tenancyApi.getStorageLimitBytes(TENANT)).thenReturn(-1L);
    }

    @Test
    void standardPlanRejectsFileOverDefaultCap() throws Exception {
        when(tenancyApi.getMaxUploadFileSizeBytes(TENANT)).thenReturn(50 * MB); // default

        assertThatThrownBy(() ->
                service.uploadDocument(fileOf(60 * MB), 2L, TENANT, "t", "d", true))
                .isInstanceOf(FileTooLargeException.class)
                .hasMessageContaining("exceeds the per-file limit");

        verify(storage, never()).uploadFile(any(), anyLong());
    }

    @Test
    void enterprisePlanAcceptsLargeFile() throws Exception {
        when(tenancyApi.getMaxUploadFileSizeBytes(TENANT)).thenReturn(5L * 1024 * MB); // 5 GB
        stubSuccessfulUpload();

        service.uploadDocument(fileOf(200 * MB), 2L, TENANT, "master.tif", "d", true);

        verify(storage).uploadFile(any(), anyLong());
    }

    @Test
    void fileAtExactlyTheCapIsAccepted() throws Exception {
        when(tenancyApi.getMaxUploadFileSizeBytes(TENANT)).thenReturn(50 * MB);
        stubSuccessfulUpload();

        service.uploadDocument(fileOf(50 * MB), 2L, TENANT, "t", "d", true);

        verify(storage).uploadFile(any(), anyLong());
    }
}
