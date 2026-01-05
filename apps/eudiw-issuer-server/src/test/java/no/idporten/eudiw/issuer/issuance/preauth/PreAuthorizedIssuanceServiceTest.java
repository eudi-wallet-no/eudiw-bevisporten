package no.idporten.eudiw.issuer.issuance.preauth;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialConfigurationService;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("When starting to issuing credentials in the pre-authorized code flow")
@ExtendWith(MockitoExtension.class)
public class PreAuthorizedIssuanceServiceTest {

    @Mock
    private CredentialIssuerServerProperties credentialIssuerServerProperties;

    @Mock
    private CredentialConfigurationService credentialConfigurationService;

    @Mock
    private ClaimsSourceService claimsSourceService;

    @Mock
    private AccessTokenValidationService accessTokenValidationService;

    @Mock
    private CredentialIssuanceStatusService credentialIssuanceStatusService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PreAuthorizedIssuanceService preAuthorizedIssuanceService;

    @DisplayName("then credential configuration must support the pre-authorized code flow")
    @Test
    void testConfigurationMustSupportPreAuthorizedCodeFlow() {
        when(credentialConfigurationService.findCredentialConfiguration(eq("foo"))).thenReturn(CredentialConfigurationProperties.builder()
                .identifier("foo")
                .grantType("authorization_code")
                .build());
        PreAuthorizedIssuanceRequest preAuthorizedIssuanceRequest = PreAuthorizedIssuanceRequest.builder().credentialConfigurationId("foo").build();
        IssuerServerException e = assertThrows(IssuerServerException.class, () -> preAuthorizedIssuanceService.startIssuerTransaction(preAuthorizedIssuanceRequest, mock(JWT.class)));
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus()),
                () -> assertEquals("invalid_request", e.getError()),
                () -> assertEquals("Credential configuration can only be used with the pre-authorized code flow", e.getErrorDescription())
        );
        verifyNoInteractions(auditService);
    }

}
