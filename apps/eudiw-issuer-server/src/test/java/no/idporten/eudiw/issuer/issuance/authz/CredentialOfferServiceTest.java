package no.idporten.eudiw.issuer.issuance.authz;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialOffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("When creating credential offers for the authorization code flow")
@ActiveProfiles("junit")
@SpringBootTest
public class CredentialOfferServiceTest {

    @MockitoBean
    private AuditService auditService;

    @Autowired
    private CredentialOfferService credentialOfferService;

    @DisplayName("then a credential offer is generated for a credential configuration supporting the authorization code flow")
    @Test
    void testCreateCredentialOfferForAuthorizationCodeFlow() {
        CredentialOffer credentialOffer = credentialOfferService.createCredentialOffer("root", "junitdoc_mso_mdoc");
        assertAll(
                () -> assertEquals("https://junit.eidas2sandkasse.dev", credentialOffer.getCredentialIssuer()),
                () -> assertEquals(1, credentialOffer.getCredentialConfigurationIds().size()),
                () -> assertEquals("junitdoc_mso_mdoc", credentialOffer.getCredentialConfigurationIds().getFirst()),
                () -> assertNotNull(credentialOffer.getGrants().getAuthorizedCodeGrant()),
                () -> assertNull(credentialOffer.getGrants().getPreAuthorizedCodeGrant())
        );
        verify(auditService).logCreateCredentialOffer(eq("https://junit.eidas2sandkasse.dev"), eq(List.of("junitdoc_mso_mdoc")));
    }

    @DisplayName("then an error is returned for credential configurations supporting only the pre-authorized code flow")
    @Test
    void testDenyCredentialOfferForPreAuthorizedCodeFlow() {
        IssuerServerException e = assertThrows(IssuerServerException.class, () -> credentialOfferService.createCredentialOffer("root", "junitdoc_pre_mso_mdoc"));
        assertAll(
                () -> assertEquals(HttpStatus.BAD_REQUEST, e.getHttpStatus()),
                () -> assertEquals("invalid_request", e.getError()),
                () -> assertEquals("Credential configuration cannot be used with the authorization code flow", e.getErrorDescription())
        );
        verifyNoInteractions(auditService);
    }

}
