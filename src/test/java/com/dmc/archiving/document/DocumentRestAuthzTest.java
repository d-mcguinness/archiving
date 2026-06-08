package com.dmc.archiving.document;

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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies per-resource tenant-ownership authorization on the REST document
 * endpoints (Review C1): a caller who is not a member of a document's tenant
 * cannot read, mutate, or delete it; ADMIN may; missing -> 404.
 */
class DocumentRestAuthzTest {

    private static final Long DOC_ID = 7L;
    private static final Long DOC_TENANT = 100L;

    private final DocumentService service = mock(DocumentService.class);
    private final TenancyApi tenancyApi = mock(TenancyApi.class);
    private final DocumentController controller = new DocumentController(
            service, mock(CloudStorageService.class), mock(BillingTenantResolver.class), tenancyApi);

    private static HttpServletRequest reqAs(AuthContext ctx) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute(RestAuthInterceptor.AUTH_CONTEXT)).thenReturn(ctx);
        return req;
    }

    private Document docInTenant(Long tenantId) {
        Document d = new Document();
        d.setId(DOC_ID);
        d.setTenantId(tenantId);
        d.setStatus(DocumentStatus.ACTIVE);
        d.setCreatedAt(LocalDateTime.now());
        return d;
    }

    @Test
    void deleteByNonMemberIsForbidden_andServiceNotCalled() {
        when(service.getDocumentById(DOC_ID)).thenReturn(Optional.of(docInTenant(DOC_TENANT)));
        when(tenancyApi.isUserInTenant(3L, DOC_TENANT)).thenReturn(false);

        ResponseEntity<?> resp = controller.deleteDocument(
                DOC_ID, reqAs(new AuthContext(3L, "USER", "user")));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).deleteDocument(anyLong());
    }

    @Test
    void deleteByMemberSucceeds() {
        when(service.getDocumentById(DOC_ID)).thenReturn(Optional.of(docInTenant(DOC_TENANT)));
        when(tenancyApi.isUserInTenant(2L, DOC_TENANT)).thenReturn(true);
        when(service.deleteDocument(DOC_ID)).thenReturn(true);

        ResponseEntity<?> resp = controller.deleteDocument(
                DOC_ID, reqAs(new AuthContext(2L, "TENANT", "tenant")));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).deleteDocument(DOC_ID);
    }

    @Test
    void adminMayDeleteAnyTenantsDocument() {
        when(service.getDocumentById(DOC_ID)).thenReturn(Optional.of(docInTenant(DOC_TENANT)));
        when(service.deleteDocument(DOC_ID)).thenReturn(true);

        ResponseEntity<?> resp = controller.deleteDocument(
                DOC_ID, reqAs(new AuthContext(1L, "ADMIN", "admin")));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).deleteDocument(DOC_ID);
        verify(tenancyApi, never()).isUserInTenant(anyLong(), anyLong()); // ADMIN bypasses membership
    }

    @Test
    void getByNonMemberIsForbidden() {
        when(service.getDocumentById(DOC_ID)).thenReturn(Optional.of(docInTenant(DOC_TENANT)));
        when(tenancyApi.isUserInTenant(3L, DOC_TENANT)).thenReturn(false);

        ResponseEntity<?> resp = controller.getDocument(
                DOC_ID, reqAs(new AuthContext(3L, "USER", "user")));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void missingDocumentIsNotFound() {
        when(service.getDocumentById(DOC_ID)).thenReturn(Optional.empty());

        ResponseEntity<?> resp = controller.deleteDocument(
                DOC_ID, reqAs(new AuthContext(1L, "ADMIN", "admin")));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(service, never()).deleteDocument(anyLong());
    }
}
