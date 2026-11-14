package com.dmc.archiving.document;

import com.dmc.archiving.archive.api.ArchiveAccessApi;
import com.dmc.archiving.archive.api.ArchiveAccessInfo;
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
    private static final Long ARCHIVE_ID = 55L;

    private final DocumentService service = mock(DocumentService.class);
    private final TenancyApi tenancyApi = mock(TenancyApi.class);
    private final ArchiveAccessApi archiveAccessApi = mock(ArchiveAccessApi.class);
    private final DocumentController controller = new DocumentController(
            service, mock(CloudStorageService.class), mock(BillingTenantResolver.class), tenancyApi,
            archiveAccessApi);

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

    // ---- associate-archive: tenant membership alone must not allow any archive id ----

    private void memberOfDocTenant(Long userId) {
        when(service.getDocumentById(DOC_ID)).thenReturn(Optional.of(docInTenant(DOC_TENANT)));
        when(tenancyApi.isUserInTenant(userId, DOC_TENANT)).thenReturn(true);
    }

    @Test
    void memberCannotLinkToArchiveTheyAreNotAssignedTo() {
        memberOfDocTenant(3L);
        when(archiveAccessApi.accessFor(ARCHIVE_ID, 3L))
                .thenReturn(new ArchiveAccessInfo(ARCHIVE_ID, DOC_TENANT, false));

        ResponseEntity<?> resp = controller.associateWithArchive(
                DOC_ID, ARCHIVE_ID, reqAs(new AuthContext(3L, "USER", "user")));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).associateWithArchive(anyLong(), anyLong());
    }

    @Test
    void memberMayLinkToArchiveTheyOwnOrAreAssignedTo() {
        memberOfDocTenant(3L);
        when(archiveAccessApi.accessFor(ARCHIVE_ID, 3L))
                .thenReturn(new ArchiveAccessInfo(ARCHIVE_ID, DOC_TENANT, true));
        when(service.associateWithArchive(DOC_ID, ARCHIVE_ID)).thenReturn(docInTenant(DOC_TENANT));

        ResponseEntity<?> resp = controller.associateWithArchive(
                DOC_ID, ARCHIVE_ID, reqAs(new AuthContext(3L, "USER", "user")));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).associateWithArchive(DOC_ID, ARCHIVE_ID);
    }

    @Test
    void memberCannotLinkToAssignedArchiveInAnotherTenant() {
        memberOfDocTenant(3L);
        when(archiveAccessApi.accessFor(ARCHIVE_ID, 3L))
                .thenReturn(new ArchiveAccessInfo(ARCHIVE_ID, 999L, true));

        ResponseEntity<?> resp = controller.associateWithArchive(
                DOC_ID, ARCHIVE_ID, reqAs(new AuthContext(3L, "USER", "user")));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).associateWithArchive(anyLong(), anyLong());
    }

    @Test
    void tenantManagerMayLinkToAnyArchiveInTheirTenant_butNotAnotherTenants() {
        memberOfDocTenant(2L);
        when(archiveAccessApi.accessFor(ARCHIVE_ID, 2L))
                .thenReturn(new ArchiveAccessInfo(ARCHIVE_ID, DOC_TENANT, false));
        when(archiveAccessApi.accessFor(56L, 2L))
                .thenReturn(new ArchiveAccessInfo(56L, 999L, false));
        when(service.associateWithArchive(DOC_ID, ARCHIVE_ID)).thenReturn(docInTenant(DOC_TENANT));
        AuthContext manager = new AuthContext(2L, "TENANT", "tenant");

        assertThat(controller.associateWithArchive(DOC_ID, ARCHIVE_ID, reqAs(manager)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(controller.associateWithArchive(DOC_ID, 56L, reqAs(manager)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        verify(service, never()).associateWithArchive(DOC_ID, 56L);
    }

    @Test
    void adminMayLinkToAnyExistingArchive_andMissingArchiveIsNotFound() {
        when(service.getDocumentById(DOC_ID)).thenReturn(Optional.of(docInTenant(DOC_TENANT)));
        when(archiveAccessApi.accessFor(ARCHIVE_ID, 1L))
                .thenReturn(new ArchiveAccessInfo(ARCHIVE_ID, 999L, false));
        when(archiveAccessApi.accessFor(404L, 1L)).thenReturn(null);
        when(service.associateWithArchive(DOC_ID, ARCHIVE_ID)).thenReturn(docInTenant(DOC_TENANT));
        AuthContext admin = new AuthContext(1L, "ADMIN", "admin");

        assertThat(controller.associateWithArchive(DOC_ID, ARCHIVE_ID, reqAs(admin)).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(controller.associateWithArchive(DOC_ID, 404L, reqAs(admin)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(service, never()).associateWithArchive(DOC_ID, 404L);
    }
}
