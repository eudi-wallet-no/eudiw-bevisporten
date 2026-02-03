package no.idporten.eudiw.issuer.oauth2.integration;

import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialConfigurationService;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.issuance.preauth.PreAuthorizedIssuanceRequest;
import no.idporten.eudiw.issuer.issuance.preauth.integration.PreAuthorizationIntegration;
import no.idporten.eudiw.issuer.openid4vci.protocol.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.MockServerRestClientCustomizer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("When creating pre-authorizations with OIDC4VCI-aware authorization server")
@ActiveProfiles("junit")
@SpringBootTest
public class PreAuthorizationIntegrationTest {

    private PreAuthorizationIntegration preAuthorizationIntegration;

    @Autowired
    private CredentialIssuerServerProperties credentialIssuerServerProperties;

    @Autowired
    private CredentialConfigurationService credentialConfigurationService;

    private MockServerRestClientCustomizer customizer;

    @BeforeEach
    public void setUp() {
        customizer = new MockServerRestClientCustomizer();
        RestClient.Builder builder = RestClient.builder();
        customizer.customize(builder);
        preAuthorizationIntegration = new PreAuthorizationIntegration(credentialIssuerServerProperties, credentialConfigurationService, builder.build());
    }

    @DisplayName("then a pre-authorization request containing user and transaction information is sent")
    @Test
    void testCreatePreAuthorization() {
        final String subjectIdentifier = "11111111111";
        final String credentialConfigurationId = "junitdoc_pre_mso_mdoc";
        final CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(credentialConfigurationId);
        credentialConfigurationProperties.setPreAuthorizationLifetime(Duration.ofMinutes(3));
        final IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        PreAuthorizedIssuanceRequest preAuthorizedIssuanceRequest = PreAuthorizedIssuanceRequest.builder()
                .credentialConfigurationId(credentialConfigurationId)
                .credentialConfigurationId(credentialConfigurationId)
                .credentialIssuer("https://junit.eidas2sandkasse.dev/")
                .subject(new Subject(subjectIdentifier))
                .build();
        final String validResponse = """
                {
                    "pre-authorized_code": "pac",
                    "expires_in": 3
                }""";
        customizer.getServer()
                .expect(requestTo("/api/v1/pre-authorizations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.aud").value("https://junit.eidas2sandkasse.dev/"))
                .andExpect(jsonPath("$.scope[0]").value("eudiw:junit"))
                .andExpect(jsonPath("$.sub").value(subjectIdentifier))
                .andExpect(jsonPath("$.tx_id").value(issuanceTransactionId.getValue()))
                .andExpect(jsonPath("$.authorization_lifetime").value(60 * 3))
                .andRespond(withSuccess(validResponse, MediaType.APPLICATION_JSON));
        String preAuthorizedCode = preAuthorizationIntegration.preAuthorize(issuanceTransactionId, preAuthorizedIssuanceRequest);
        customizer.getServer().verify();
        assertEquals("pac", preAuthorizedCode);
    }

}
