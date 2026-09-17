package no.idporten.eudiw.issuer.openid4vci;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenantService;
import no.idporten.eudiw.issuer.openid4vci.metadata.CredentialIssuerMetadata;
import no.idporten.logging.audit.AuditLogger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("When creating credential issuer metadata")
@ActiveProfiles("junit")
@SpringBootTest
class CredentialIssuerMetadataServiceTest {

    private static final String ROOT_ISSUER = "https://junit.eidas2sandkasse.dev";

    @Autowired
    private CredentialIssuerMetadataService credentialIssuerMetadataService;

    @Autowired
    private CredentialIssuerServerProperties credentialIssuerServerProperties;

    @Autowired
    private CredentialIssuerTenantService credentialIssuerTenantService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuditLogger auditLogger;

    @DisplayName("then signed metadata contains the required issuer claims and validity")
    @Test
    void testSignedMetadata() throws Exception {
        Instant beforeSigning = Instant.now();

        SignedJWT signedMetadata = SignedJWT.parse(
                credentialIssuerMetadataService.getSignedCredentialIssuerMetadata(null));
        JWTClaimsSet claims = signedMetadata.getJWTClaimsSet();

        assertAll(
                () -> assertFalse(signedMetadata.getSignature().toString().isBlank()),
                () -> assertEquals(ROOT_ISSUER, claims.getSubject()),
                () -> assertEquals(ROOT_ISSUER, claims.getIssuer()),
                () -> assertEquals(ROOT_ISSUER, claims.getStringClaim("credential_issuer")),
                () -> assertFalse(claims.getIssueTime().toInstant().isBefore(beforeSigning.minusSeconds(1))),
                () -> assertFalse(claims.getIssueTime().toInstant().isAfter(Instant.now().plusSeconds(1))),
                () -> assertEquals(
                        Duration.ofMinutes(5),
                        Duration.between(claims.getIssueTime().toInstant(), claims.getExpirationTime().toInstant())));
    }

    @DisplayName("then requesting signed metadata for a tenant without a signing keystore is rejected")
    @Test
    void testSignedMetadataWithoutSigningKeystore() {
        IssuerServerException exception = assertThrows(
                IssuerServerException.class,
                () -> credentialIssuerMetadataService.getSignedCredentialIssuerMetadata("junit"));

        assertAll(
                () -> assertEquals(HttpStatus.NOT_ACCEPTABLE, exception.getHttpStatus()),
                () -> assertEquals("invalid_request", exception.getError()));
    }

    @DisplayName("then issuer_info is omitted for a tenant without a registration certificate")
    @Test
    void testCredentialIssuerMetadataWithoutIssuerInfo() {
        CredentialIssuerTenant tenant = credentialIssuerTenantService.findTenantById("junit");

        CredentialIssuerMetadata metadata = credentialIssuerMetadataService.credentialIssuerMetadata(credentialIssuerServerProperties, tenant);
        Map<String, Object> metadataClaims = objectMapper.convertValue(metadata, new TypeReference<>() {});

        assertFalse(metadataClaims.containsKey("issuer_info"));
    }

    @DisplayName("then issuer_info contains the registration certificate and registrar dataset for a tenant with a registration certificate")
    @Test
    @SuppressWarnings("unchecked")
    void testCredentialIssuerMetadataWithIssuerInfo() {
        CredentialIssuerTenant tenant = credentialIssuerTenantService.findTenantById("webuild");

        CredentialIssuerMetadata metadata = credentialIssuerMetadataService.credentialIssuerMetadata(credentialIssuerServerProperties, tenant);
        Map<String, Object> metadataClaims = objectMapper.convertValue(metadata, new TypeReference<>() {});
        List<Map<String, Object>> issuerInfo = (List<Map<String, Object>>) metadataClaims.get("issuer_info");

        Map<String, Object> registrationCert = issuerInfo.get(0);
        Map<String, Object> registrarDataset = issuerInfo.get(1);
        Map<String, Object> registrarDatasetData = (Map<String, Object>) registrarDataset.get("data");
        List<Map<String, Object>> identifiers = (List<Map<String, Object>>) registrarDatasetData.get("identifier");

        assertAll(
                () -> assertEquals(2, issuerInfo.size()),
                () -> assertEquals("registration_cert", registrationCert.get("format")),
                () -> assertEquals(tenant.getRegistrationCertificate().registrationCertificateJwt(), registrationCert.get("data")),
                () -> assertEquals("registrar_dataset", registrarDataset.get("format")),
                () -> assertEquals("NO-ORG-123456789", identifiers.getFirst().get("identifier")),
                () -> assertEquals("https://registrar.example.no/", registrarDatasetData.get("registryURI")));
    }
}


