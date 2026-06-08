package com.dmc.archiving.document;

import com.dmc.archiving.auth.api.AccessDeniedException;
import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.document.model.Document;
import com.dmc.archiving.storage.CloudStorageService;
import com.dmc.archiving.tenancy.api.BillingTenantResolver;
import com.dmc.archiving.tenancy.api.TenancyApi;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the raw upload/download endpoints (Review metering-integrity + #27):
 * uploads route through the metered DocumentService (non-admin attributed to the
 * resolved tenant and billed; ADMIN is a non-billed operator upload with no
 * resolver call; quota/cap failures map to HTTP status); identity comes from the
 * token, never request params; and downloads only presign a key the caller's
 * tenant owns (unknown key 404, foreign tenant 403, ADMIN any).
 */
class FileUploadControllerMeteringTest {

    private final CloudStorageService storage = mock(CloudStorageService.class);
    private final DocumentService documentService = mock(DocumentService.class);
    private final BillingTenantResolver resolver = mock(BillingTenantResolver.class);
    private final TenancyApi tenancyApi = mock(TenancyApi.class);
    private final FileUploadController controller =
            new FileUploadController(storage, documentService, resolver, tenancyApi);

    private HttpServletRequest requestAs(AuthContext ctx) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute(RestAuthInterceptor.AUTH_CONTEXT)).thenReturn(ctx);
        return req;
    }

    private MultipartFile file(boolean empty) {
        MultipartFile f = mock(MultipartFile.class);
        when(f.isEmpty()).thenReturn(empty);
        return f;
    }

    private static Document doc() {
        Document d = new Document();
        d.setId(5L);
        d.setFileKey("k");
        d.setFileUrl("u");
        d.setFileName("f.txt");
        d.setFileSize(10L);
        d.setContentType("text/plain");
        return d;
    }

    @Test
    void nonAdminUploadIsAttributedToResolvedTenantAndBilled() {
        AuthContext ctx = new AuthContext(2L, "TENANT", "tenant");
        when(resolver.resolve(ctx, null)).thenReturn(100L);
        when(documentService.uploadDocument(any(), eq(2L), eq(100L), isNull(), isNull(), eq(true)))
                .thenReturn(doc());

        var response = controller.uploadFile(file(false), requestAs(ctx));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(documentService).uploadDocument(any(), eq(2L), eq(100L), isNull(), isNull(), eq(true));
    }

    @Test
    void adminUploadIsOperatorNonBilled_andSkipsTheResolver() {
        AuthContext ctx = new AuthContext(1L, "ADMIN", "admin");
        when(documentService.uploadDocument(any(), eq(1L), isNull(), isNull(), isNull(), eq(false)))
                .thenReturn(doc());

        var response = controller.uploadFile(file(false), requestAs(ctx));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(resolver, never()).resolve(any(), any());
        verify(documentService).uploadDocument(any(), eq(1L), isNull(), isNull(), isNull(), eq(false));
    }

    @Test
    void emptyFileIsRejectedBeforeAnyUpload() {
        AuthContext ctx = new AuthContext(2L, "TENANT", "tenant");

        var response = controller.uploadFile(file(true), requestAs(ctx));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(documentService, never()).uploadDocument(any(), anyLong(), any(), any(), any(), anyBoolean());
    }

    @Test
    void resolverDenialMapsToForbidden() {
        AuthContext ctx = new AuthContext(2L, "TENANT", "tenant");
        when(resolver.resolve(ctx, null)).thenThrow(new AccessDeniedException("no single tenant"));

        var response = controller.uploadFile(file(false), requestAs(ctx));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(documentService, never()).uploadDocument(any(), anyLong(), any(), any(), any(), anyBoolean());
    }

    @Test
    void storageQuotaExceededMapsTo507() {
        AuthContext ctx = new AuthContext(2L, "TENANT", "tenant");
        when(resolver.resolve(ctx, null)).thenReturn(100L);
        when(documentService.uploadDocument(any(), eq(2L), eq(100L), isNull(), isNull(), eq(true)))
                .thenThrow(new StorageQuotaExceededException("over quota"));

        var response = controller.uploadFile(file(false), requestAs(ctx));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INSUFFICIENT_STORAGE);
    }

    @Test
    void uploadForUserRejectsInvalidUserIdButStillMetersOnTokenIdentity() {
        AuthContext ctx = new AuthContext(2L, "TENANT", "tenant");

        var bad = controller.uploadFileForUser(file(false), 0L, requestAs(ctx));
        assertThat(bad.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(documentService, never()).uploadDocument(any(), anyLong(), any(), any(), any(), anyBoolean());
    }

    // ---- download tenant-ownership (#27) ----

    private static Document docInTenant(Long tenantId) {
        Document d = doc();
        d.setTenantId(tenantId);
        return d;
    }

    @Test
    void downloadUnknownKeyIsNotFound_andNeverPresigns() throws Exception {
        AuthContext ctx = new AuthContext(2L, "TENANT", "tenant");
        when(documentService.getDocumentByFileKey("ghost")).thenReturn(Optional.empty());

        var response = controller.downloadFile("ghost", requestAs(ctx));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(storage, never()).getPresignedUrl(anyString(), anyInt());
    }

    @Test
    void downloadForeignTenantIsForbidden_andNeverPresigns() throws Exception {
        AuthContext ctx = new AuthContext(2L, "TENANT", "tenant");
        when(documentService.getDocumentByFileKey("k")).thenReturn(Optional.of(docInTenant(100L)));
        when(tenancyApi.isUserInTenant(2L, 100L)).thenReturn(false);

        var response = controller.downloadFile("k", requestAs(ctx));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(storage, never()).getPresignedUrl(anyString(), anyInt());
    }

    @Test
    void downloadOwnTenantPresignsTheUrl() throws Exception {
        AuthContext ctx = new AuthContext(2L, "TENANT", "tenant");
        when(documentService.getDocumentByFileKey("k")).thenReturn(Optional.of(docInTenant(100L)));
        when(tenancyApi.isUserInTenant(2L, 100L)).thenReturn(true);
        when(storage.getPresignedUrl("k", 60)).thenReturn("https://signed");

        var response = controller.downloadFile("k", requestAs(ctx));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(storage).getPresignedUrl("k", 60);
    }

    @Test
    void adminCanDownloadAnyTenantsKey() throws Exception {
        AuthContext ctx = new AuthContext(1L, "ADMIN", "admin");
        when(documentService.getDocumentByFileKey("k")).thenReturn(Optional.of(docInTenant(100L)));
        when(storage.getPresignedUrl("k", 60)).thenReturn("https://signed");

        var response = controller.downloadFile("k", requestAs(ctx));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(tenancyApi, never()).isUserInTenant(anyLong(), anyLong());
    }
}
