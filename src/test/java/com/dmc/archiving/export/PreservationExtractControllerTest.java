package com.dmc.archiving.export;

import com.dmc.archiving.auth.api.AuthContext;
import com.dmc.archiving.tenancy.api.TenancyApi;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreservationExtractControllerTest {

    private final PreservationExtractService service = mock(PreservationExtractService.class);
    private final TenancyApi tenancy = mock(TenancyApi.class);
    private final PreservationExtractController controller = new PreservationExtractController(service, tenancy);

    private static MockHttpServletRequest as(AuthContext ctx) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        if (ctx != null) r.setAttribute(PreservationExtractController.AUTH_CONTEXT, ctx);
        return r;
    }

    private static final AuthContext TENANT_USER = new AuthContext(5L, "TENANT", "tenant");
    private static final AuthContext ADMIN = new AuthContext(1L, "ADMIN", "admin");
    private static final PreservationExtractService.Rendered RENDERED =
            new PreservationExtractService.Rendered("{}".getBytes(), "preservation_7_EARK_extract.json", "application/json", 100L);

    @Test
    void anonymousIsRejectedBeforeAnyWork() {
        ResponseEntity<?> r = controller.extract(7L, null, Map.of("password", "x"), as(null));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(service, never()).render(anyLong(), any());
    }

    @Test
    void passwordIsRequired() {
        ResponseEntity<?> r = controller.extract(7L, null, Map.of(), as(TENANT_USER));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.extract(7L, null, null, as(TENANT_USER)).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(service, never()).render(anyLong(), any());
    }

    @Test
    void unknownFormatIs400() {
        ResponseEntity<?> r = controller.extract(7L, "pdf", Map.of("password", "x"), as(TENANT_USER));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<?, ?>) r.getBody()).get("error").toString()).contains("pdf");
    }

    @Test
    void missingPreservationIs404() {
        when(service.render(7L, PreservationExtractService.Format.JSON)).thenReturn(Optional.empty());
        assertThat(controller.extract(7L, "json", Map.of("password", "x"), as(TENANT_USER)).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void memberOfAnotherTenantIsForbidden() {
        when(service.render(7L, PreservationExtractService.Format.JSON)).thenReturn(Optional.of(RENDERED));
        when(tenancy.isUserInTenant(5L, 100L)).thenReturn(false);
        assertThat(controller.extract(7L, null, Map.of("password", "x"), as(TENANT_USER)).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void tenantMemberGetsTheDownload() {
        when(service.render(7L, PreservationExtractService.Format.JSON)).thenReturn(Optional.of(RENDERED));
        when(tenancy.isUserInTenant(5L, 100L)).thenReturn(true);

        ResponseEntity<?> r = controller.extract(7L, null, Map.of("password", "x"), as(TENANT_USER));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType().toString()).isEqualTo("application/json");
        assertThat(r.getHeaders().getContentDisposition().getFilename()).isEqualTo("preservation_7_EARK_extract.json");
        assertThat((byte[]) r.getBody()).isEqualTo("{}".getBytes());
    }

    @Test
    void adminBypassesTenantCheckAndCanAskForXml() {
        PreservationExtractService.Rendered xml = new PreservationExtractService.Rendered(
                "<x/>".getBytes(), "preservation_7_EARK_extract.xml", "application/xml", 100L);
        when(service.render(7L, PreservationExtractService.Format.XML)).thenReturn(Optional.of(xml));

        ResponseEntity<?> r = controller.extract(7L, "xml", Map.of("password", "x"), as(ADMIN));

        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(r.getHeaders().getContentType().toString()).isEqualTo("application/xml");
        assertThat(r.getHeaders().getContentDisposition().getFilename()).endsWith(".xml");
        verify(tenancy, never()).isUserInTenant(anyLong(), anyLong());
    }
}
