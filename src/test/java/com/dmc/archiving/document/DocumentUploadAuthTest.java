package com.dmc.archiving.document;

import com.dmc.archiving.auth.api.AccessDeniedException;
import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.document.model.Document;
import com.dmc.archiving.document.model.DocumentStatus;
import com.dmc.archiving.storage.CloudStorageService;
import com.dmc.archiving.tenancy.api.BillingTenantResolver;
import com.dmc.archiving.tenancy.api.TenancyApi;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies REST document-upload identity + attribution (Risk 2a-REST):
 * identity comes from the authenticated context (stashed by the interceptor),
 * the tenant is resolved (not forged via params), and ADMIN uploads are
 * non-billable. Unauthenticated rejection is the interceptor's job
 * (see RestAuthInterceptorTest).
 */
class DocumentUploadAuthTest {

    private final DocumentService service = mock(DocumentService.class);
    private final BillingTenantResolver resolver = mock(BillingTenantResolver.class);
    private final TenancyApi tenancyApi = mock(TenancyApi.class);
    private final DocumentController controller =
            new DocumentController(service, mock(CloudStorageService.class), resolver, tenancyApi);

    private static MultipartFile nonEmptyFile() {
        MultipartFile f = mock(MultipartFile.class);
        when(f.isEmpty()).thenReturn(false);
        return f;
    }

    private static HttpServletRequest reqAs(Long userId, String role) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute(RestAuthInterceptor.AUTH_CONTEXT))
                .thenReturn(new AuthContext(userId, role, role.toLowerCase()));
        return req;
    }

    private static Document savedDoc() {
        Document d = new Document();
        d.setId(1L);
        d.setStatus(DocumentStatus.ACTIVE);
        d.setCreatedAt(LocalDateTime.now());
        return d;
    }

    @Test
    void rejectsClaimToTenantTheCallerIsNotAMemberOf() {
        when(resolver.resolve(any(), eq(999L)))
                .thenThrow(new AccessDeniedException("not a member of tenant 999"));

        ResponseEntity<?> resp = controller.uploadDocument(
                nonEmptyFile(), 999L, null, null, null, reqAs(3L, "USER"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).uploadDocument(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void attributesUploadToTokenIdentityAndResolvedTenant_billableForTenant() {
        when(resolver.resolve(any(), any())).thenReturn(100L);
        when(service.uploadDocument(any(), eq(2L), eq(100L), any(), any(), eq(true)))
                .thenReturn(savedDoc());

        ResponseEntity<?> resp = controller.uploadDocument(
                nonEmptyFile(), 100L, null, "title", "desc", reqAs(2L, "TENANT"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // userId 2 from the token (not a request param), tenant 100, billable.
        verify(service).uploadDocument(any(), eq(2L), eq(100L), eq("title"), eq("desc"), eq(true));
    }

    @Test
    void adminUploadIsNotBillable() {
        when(resolver.resolve(any(), any())).thenReturn(100L);
        when(service.uploadDocument(any(), eq(1L), eq(100L), any(), any(), eq(false)))
                .thenReturn(savedDoc());

        ResponseEntity<?> resp = controller.uploadDocument(
                nonEmptyFile(), 100L, null, "title", "desc", reqAs(1L, "ADMIN"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).uploadDocument(any(), eq(1L), eq(100L), any(), any(), eq(false));
    }
}
