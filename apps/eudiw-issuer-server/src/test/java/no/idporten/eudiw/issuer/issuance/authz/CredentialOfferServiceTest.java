package no.idporten.eudiw.issuer.issuance.authz;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialConfigurationService;
import no.idporten.eudiw.issuer.config.CredentialIssuerContext;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.config.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialOffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("When creating credential offers for the authorization code flow")
@ExtendWith(MockitoExtension.class)
public class CredentialOfferServiceTest {

    @Mock
    private CredentialIssuerServerProperties credentialIssuerServerProperties;

    @Mock
    private CredentialConfigurationService credentialConfigurationService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CredentialOfferService credentialOfferService;

    @DisplayName("then a credential offer is generated for a credential configuration supporting the authorization code flow")
    @Test
    void testCreateCredentialOfferForAuthorizationCodeFlow() {
        when(credentialIssuerServerProperties.getCredentialIssuer()).thenReturn(URI.create("https://junit.issuer.idporten.no"));
        when(credentialConfigurationService.findCredentialConfiguration(eq("foo"))).thenReturn(ExtendedCredentialConfiguration.builder()
                .credentialConfigurationId("foo")
                .credentialIssuerContext(CredentialIssuerContext.builder().grantType("authorization_code").build())
                .build());
        CredentialOffer credentialOffer = credentialOfferService.createCredentialOffer("foo");
        assertAll(
                () -> assertEquals("https://junit.issuer.idporten.no", credentialOffer.getCredentialIssuer()),
                () -> assertEquals(1, credentialOffer.getCredentialConfigurationIds().size()),
                () -> assertEquals("foo", credentialOffer.getCredentialConfigurationIds().getFirst()),
                () -> assertNotNull(credentialOffer.getGrants().getAuthorizedCodeGrant()),
                () -> assertNull(credentialOffer.getGrants().getPreAuthorizedCodeGrant())
        );
        verify(auditService).logCreateCredentialOffer(eq("https://junit.issuer.idporten.no"), eq(List.of("foo")));
    }

    @DisplayName("then an error is returned for credential configurations supporting only the pre-authorized code flow")
    @Test
    void testDenyCredentialOfferForPreAuthorizedCodeFlow() {
        when(credentialConfigurationService.findCredentialConfiguration(eq("foo"))).thenReturn(ExtendedCredentialConfiguration.builder()
                .credentialConfigurationId("foo")
                .credentialIssuerContext(CredentialIssuerContext.builder().grantType("urn:ietf:params:oauth:grant-type:pre-authorized_code").build())
                .build());
        IssuerServerException e = assertThrows(IssuerServerException.class, () -> credentialOfferService.createCredentialOffer("foo"));
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus()),
                () -> assertEquals("invalid_request", e.getError()),
                () -> assertEquals("Credential configuration cannot be used with the authorization code flow", e.getErrorDescription())
        );
        verifyNoInteractions(auditService);
    }

}
