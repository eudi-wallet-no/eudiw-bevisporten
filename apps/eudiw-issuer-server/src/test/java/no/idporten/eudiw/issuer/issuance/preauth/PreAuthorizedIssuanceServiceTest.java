package no.idporten.eudiw.issuer.issuance.preauth;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.CredentialIssuerContext;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("When starting to issuing credentials in the pre-authorized code flow")
@ExtendWith(MockitoExtension.class)
public class PreAuthorizedIssuanceServiceTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PreAuthorizedIssuanceService preAuthorizedIssuanceService;

    @DisplayName("then credential configuration must support the pre-authorized code flow")
    @Test
    void testConfigurationMustSupportPreAuthorizedCodeFlow() {
        CredentialIssuerTenant credentialIssuerTenant = spy(CredentialIssuerTenant.builder().credentialIssuer(URI.create("https://junit.eidas2sandkasse.dev/foo")).build());
        doReturn(ExtendedCredentialConfiguration.builder()
                .credentialConfigurationId("foo")
                .credentialIssuerContext(CredentialIssuerContext.builder().grantType("authorization_code").build())
                .build())
                .when(credentialIssuerTenant).findCredentialConfiguration(eq("foo"));
        PreAuthorizedIssuanceRequest preAuthorizedIssuanceRequest = PreAuthorizedIssuanceRequest.builder().credentialIssuer(credentialIssuerTenant.getCredentialIssuer().toString()).credentialConfigurationId("foo").build();
        IssuerServerException e = assertThrows(IssuerServerException.class, () -> preAuthorizedIssuanceService.startIssuerTransaction(credentialIssuerTenant, preAuthorizedIssuanceRequest, mock(JWT.class)));
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus()),
                () -> assertEquals("invalid_request", e.getError()),
                () -> assertEquals("Credential configuration can only be used with the pre-authorized code flow", e.getErrorDescription())
        );
        verifyNoInteractions(auditService);
    }
}
