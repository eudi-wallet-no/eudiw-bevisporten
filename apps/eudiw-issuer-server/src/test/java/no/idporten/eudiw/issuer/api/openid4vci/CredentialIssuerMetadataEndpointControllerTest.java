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

        @DisplayName("then a JSON error is returned when signed metadata is unavailable")
        @Test
        void testGetSignedMetadataWithoutSigningKeystore() throws Exception {
            mockMvc.perform(get("/.well-known/openid-credential-issuer/junit")
                            .accept(APPLICATION_JWT))
                    .andExpect(status().isNotAcceptable())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.error").value("invalid_request"))
                    .andExpect(jsonPath("$.error_description")
                            .value(Matchers.startsWith("Signed credential issuer metadata is not available for this tenant")));
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

    @Nested
    @DisplayName("when requesting issuer info metadata")
    class IssuerInfoMetadata {

        @DisplayName("then issuer_info is not included for a tenant without a registration certificate")
        @Test
        void testGetMetadataWithoutIssuerInfo() throws Exception {
            mockMvc.perform(get("/.well-known/openid-credential-issuer"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.issuer_info").doesNotExist());
        }

        @DisplayName("then issuer_info contains the registration certificate and registrar dataset as a JSON array")
        @Test
        void testGetMetadataWithIssuerInfo() throws Exception {
            mockMvc.perform(get("/.well-known/openid-credential-issuer/webuild"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                    .andExpect(jsonPath("$.issuer_info").isArray())
                    .andExpect(jsonPath("$.issuer_info.length()").value(2))
                    .andExpect(jsonPath("$.issuer_info[0].format").value("registration_cert"))
                    .andExpect(jsonPath("$.issuer_info[0].data").value("eyJhbGciOiJFUzI1NiJ9.junit-registration-certificate-jwt.signature"))
                    .andExpect(jsonPath("$.issuer_info[1].format").value("registrar_dataset"))
                    .andExpect(jsonPath("$.issuer_info[1].data.identifier[0].identifier").value("NO-ORG-123456789"))
                    .andExpect(jsonPath("$.issuer_info[1].data.identifier[0].type").value("http://data.europa.eu/eudi/id/EUID"))
                    .andExpect(jsonPath("$.issuer_info[1].data.srvDescription[0].lang").value("no"))
                    .andExpect(jsonPath("$.issuer_info[1].data.srvDescription[1].lang").value("en"))
                    .andExpect(jsonPath("$.issuer_info[1].data.registryURI").value("https://registrar.example.no/"))
                    .andExpect(jsonPath("$.issuer_info[1].data.providesAttestations[0].format").value("dc+sd-jwt"))
                    .andExpect(jsonPath("$.issuer_info[1].data.providesAttestations[0].type").value("urn:eudi:pid:1"))
                    .andExpect(jsonPath("$.issuer_info[1].data.providesAttestations[1].format").value("mso_mdoc"))
                    .andExpect(jsonPath("$.issuer_info[1].data.providesAttestations[1].type").value("eu.europa.ec.eudi.pid.1"));
        }
    }

}
