package no.idporten.eudiw.issuer.api.openid4vci;


import no.idporten.eudiw.issuer.openid4vci.CredentialIssuerMetadataService;
import no.idporten.logging.audit.AuditLogger;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("When accessing the credential issuer metadata endpoint")
@AutoConfigureMockMvc
@ActiveProfiles("junit")
@SpringBootTest
public class CredentialIssuerMetadataEndpointControllerTest {

    private static final MediaType APPLICATION_JWT = MediaType.parseMediaType("application/jwt");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogger auditLogger;

    @MockitoSpyBean
    private CredentialIssuerMetadataService credentialIssuerMetadataService;

    @BeforeEach
    void setUp() {
        credentialIssuerMetadataService.refreshCredentialIssuerMetadata();
    }

    @Nested
    @DisplayName("when requesting unsigned JSON metadata")
    class UnsignedMetadata {

        @DisplayName("then the issuers metadata is returned in a JSON format")
        @Test
        void testGetMetadata() throws Exception {
            mockMvc.perform(get("/.well-known/openid-credential-issuer"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.credential_issuer").value("https://junit.eidas2sandkasse.dev"))
                    .andExpect(jsonPath("$.authorization_servers.[0]").value("https://junit.idporten.no"))
                    .andExpect(jsonPath("$.credential_endpoint").value("https://junit.eidas2sandkasse.dev/openid4vci/credential"))
                    .andExpect(jsonPath("$.nonce_endpoint").value("https://junit.eidas2sandkasse.dev/openid4vci/nonce"))
                    .andExpect(jsonPath("$.batch_credential_issuance").doesNotExist());
        }

        @DisplayName("then JSON metadata is returned when explicitly requested")
        @Test
        void testGetJsonMetadata() throws Exception {
            mockMvc.perform(get("/.well-known/openid-credential-issuer")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @DisplayName("then JSON metadata is returned for a wildcard accept header")
        @Test
        void testGetMetadataWithWildcardAcceptHeader() throws Exception {
            mockMvc.perform(get("/.well-known/openid-credential-issuer")
                            .accept(MediaType.ALL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("when requesting signed metadata")
    class SignedMetadata {

        @DisplayName("then signed metadata is returned when JWT is requested")
        @Test
        void testGetSignedMetadata() throws Exception {
            mockMvc.perform(get("/.well-known/openid-credential-issuer")
                            .accept(APPLICATION_JWT))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(APPLICATION_JWT))
                    .andExpect(content().string(Matchers.matchesPattern("[^.]+\\.[^.]+\\.[^.]+")));
        }

        @DisplayName("then the preferred supported media type is returned")
        @Test
        void testGetPreferredMetadataMediaType() throws Exception {
            mockMvc.perform(get("/.well-known/openid-credential-issuer")
                            .header("Accept", "application/json;q=0.5, application/jwt;q=0.9"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(APPLICATION_JWT));
        }
    }

    @Nested
    @DisplayName("when requesting batch issuance metadata")
    class BatchIssuanceMetadata {

        @DisplayName("then batch issuance is only included if batch size is 2 or greater")
        @Test
        void testGetMetadataWithBatchIssuance() throws Exception {
            mockMvc.perform(get("/.well-known/openid-credential-issuer/junit"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.batch_credential_issuance.batch_size").value(5));
        }
    }

    @Nested
    @DisplayName("when requesting credential metadata content")
    class CredentialMetadataContent {

        @DisplayName("then credential configurations metadata is created from issuer server, credentials configuration and claims sources config ")
        @Test
        void testCredentialConfigurationsSupportedBuiltFromApplicationConfiguration() throws Exception {
            mockMvc.perform(get("/.well-known/openid-credential-issuer"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.credential_configurations_supported").exists())
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_mso_mdoc']").exists())
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_mso_mdoc']['doctype']").value("junitdoc"))
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_mso_mdoc']['vct']").doesNotExist())
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_mso_mdoc']['scope']").value("eudiw:junit"))
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_mso_mdoc']['format']").value("mso_mdoc"))
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_mso_mdoc']['cryptographic_binding_methods_supported'][0]").value("jwk"))
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_mso_mdoc']['credential_signing_alg_values_supported'][0]").value(-7))
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_mso_mdoc']['credential_metadata']['display'][0]['name']").value("Junit doc"))
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_mso_mdoc']['credential_metadata']['display'][0]['description']").value("Kun for junit-tester"))
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_mso_mdoc']['credential_metadata']['display'][0]['background_color']").value("#afcee9"))
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_mso_mdoc']['credential_metadata']['display'][0]['text_color']").value("#002c54"))
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_mso_mdoc']['credential_metadata']['claims'][0]['path'][0]").value("junitdoc"))
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_mso_mdoc']['credential_metadata']['claims'][0]['path'][1]").value("attr1"))
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_sd_jwt_vc']").exists())
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_sd_jwt_vc']['doctype']").doesNotExist())
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_sd_jwt_vc']['vct']").value("junitdoc"))
                    .andExpect(jsonPath("$['credential_configurations_supported']['junitdoc_sd_jwt_vc']['credential_signing_alg_values_supported'][0]").value("ES256"));
        }
    }

}
