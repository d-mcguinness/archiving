package com.dmc.archiving.export;

import com.dmc.archiving.archive.api.ArchiveExportApi;
import com.dmc.archiving.archive.api.ArchiveExportInfo;
import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.common.exception.ResourceNotFoundException;
import com.dmc.archiving.intake.api.IntakeExportApi;
import com.dmc.archiving.preservation.api.PreservationExportApi;
import com.dmc.archiving.release.api.ReleaseExportApi;
import com.dmc.archiving.tenancy.api.TenancyApi;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Who may call POST /api/archives/{id}/extract and /export-package. */
class ArchiveExportControllerTest {

    private static final AuthContext MEMBER = new AuthContext(5L, "USER", "member");
    private static final AuthContext ADMIN = new AuthContext(1L, "ADMIN", "admin");
    private static final ArchiveExportInfo INFO =
            new ArchiveExportInfo(7L, 100L, "Title", "EARK", "ref", Map.of("title", "Title"));

    private final ArchiveExportApi archives = mock(ArchiveExportApi.class);
    private final IntakeExportApi intakes = mock(IntakeExportApi.class);
    private final PreservationExportApi preservations = mock(PreservationExportApi.class);
    private final ReleaseExportApi releases = mock(ReleaseExportApi.class);
    private final TenancyApi tenancy = mock(TenancyApi.class);
    private final ArchiveExtractService extracts = mock(ArchiveExtractService.class);
    private final ArchiveExportController controller =
            new ArchiveExportController(archives, intakes, preservations, releases, tenancy, extracts);

    private static final Map<String, String> PASSWORD = Map.of("password", "x");
    private static final ArchiveExtractService.Rendered RENDERED = new ArchiveExtractService.Rendered(
            "{\"title\":\"secret\"}".getBytes(), "archive_7_EARK_extract.json", "application/json", 100L);

    private static MockHttpServletRequest as(AuthContext ctx) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        if (ctx != null) r.setAttribute(PreservationExtractController.AUTH_CONTEXT, ctx);
        return r;
    }

    // ---- extract ----

    @Test
    void extract_anonymousIs401BeforeAnyWork() {
        ResponseEntity<?> r = controller.extract(7L, null, PASSWORD, as(null));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(extracts);
    }

    @Test
    void extract_memberOfAnotherTenantIs403AndGetsNoArchiveData() {
        when(extracts.render(7L, PreservationExtractService.Format.JSON)).thenReturn(RENDERED);
        when(tenancy.isUserInTenant(5L, 100L)).thenReturn(false);

        ResponseEntity<?> r = controller.extract(7L, null, PASSWORD, as(MEMBER));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(r.getBody()).isInstanceOf(Map.class);
        assertThat(r.getBody().toString()).doesNotContain("secret");
    }

    @Test
    void extract_tenantMemberGetsTheDownload() {
        when(extracts.render(7L, PreservationExtractService.Format.JSON)).thenReturn(RENDERED);
        when(tenancy.isUserInTenant(5L, 100L)).thenReturn(true);

        ResponseEntity<?> r = controller.extract(7L, null, PASSWORD, as(MEMBER));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentDisposition().getFilename()).isEqualTo("archive_7_EARK_extract.json");
        assertThat((byte[]) r.getBody()).isEqualTo(RENDERED.bytes());
    }

    @Test
    void extract_adminBypassesTenantCheck() {
        when(extracts.render(7L, PreservationExtractService.Format.XML)).thenReturn(RENDERED);

        ResponseEntity<?> r = controller.extract(7L, "xml", PASSWORD, as(ADMIN));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(tenancy, never()).isUserInTenant(anyLong(), anyLong());
    }

    @Test
    void extract_missingArchiveIs404() {
        when(extracts.render(7L, PreservationExtractService.Format.JSON)).thenReturn(null);
        assertThat(controller.extract(7L, null, PASSWORD, as(MEMBER)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        when(extracts.render(8L, PreservationExtractService.Format.JSON))
                .thenThrow(new ResourceNotFoundException("Archive", 8L));
        assertThat(controller.extract(8L, null, PASSWORD, as(MEMBER)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(tenancy, never()).isUserInTenant(anyLong(), anyLong());
    }

    // ---- export-package ----

    @Test
    void anonymousIs401BeforeAnyWork() {
        ResponseEntity<?> r = controller.exportPackage(7L, Map.of(), as(null));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(archives, intakes, preservations, releases);
    }

    @Test
    void memberOfAnotherTenantIs403AndNoTenantDataIsLoaded() {
        when(archives.loadForExport(7L)).thenReturn(INFO);
        when(tenancy.isUserInTenant(5L, 100L)).thenReturn(false);

        ResponseEntity<?> r = controller.exportPackage(7L, Map.of(), as(MEMBER));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(intakes, preservations, releases);
    }

    @Test
    void tenantMemberGetsTheZip() {
        when(archives.loadForExport(7L)).thenReturn(INFO);
        when(tenancy.isUserInTenant(5L, 100L)).thenReturn(true);

        ResponseEntity<?> r = controller.exportPackage(7L, Map.of(), as(MEMBER));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType().toString()).isEqualTo("application/zip");
        assertThat((byte[]) r.getBody()).isNotEmpty();
    }

    @Test
    void adminBypassesTenantCheck() {
        when(archives.loadForExport(7L)).thenReturn(INFO);

        ResponseEntity<?> r = controller.exportPackage(7L, Map.of(), as(ADMIN));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(tenancy, never()).isUserInTenant(anyLong(), anyLong());
    }
}
