package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jose.util.X509CertUtils;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.VerificationResult;
import kotlinx.serialization.json.JsonObject;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import no.idporten.eudiw.verifier.testdata.Certificates;
import no.idporten.eudiw.verifier.testdata.VpTokenTestdata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("When handling SD-JWT credentials")
class SdJwtServiceTest {

    private static final Logger log = LogManager.getLogger(SdJwtServiceTest.class);
    @Mock VerificationResult<SDJwt> verificationResult;
    @Mock SDJwt mockedSdJwt;
    @Mock VerificationTransaction verificationTransaction;

    private SdJwtService service;

    @BeforeEach
    void setUp() {
        service = new SdJwtService(JsonMapper.builder().build());
    }

    @Test
    @DisplayName("with a compact VP token, then disclosure claims are expected")
    void parsesCompactVpTokenAndExtractsDisclosureClaims() {
        String disclosure = base64Url("[\"salt\",\"given_name\",\"Ada\"]");
        SDJwt sdJwt = service.sdJwtFromVpToken(jwt(Map.of()) + "~" + disclosure + "~");
        when(verificationResult.getSdJwt()).thenReturn(sdJwt);

        Map<String, Object> claims = service.sdJwtClaims(verificationResult);

        assertEquals(Map.of("given_name", "Ada"), claims);
    }

    @Test
    @DisplayName("with a malformed disclosure, then invalid request is expected")
    void rejectsMalformedDisclosure() {
        when(mockedSdJwt.getDisclosures()).thenReturn(java.util.Set.of("%%%"));
        when(verificationResult.getSdJwt()).thenReturn(mockedSdJwt);

        VerificationException exception =
                assertThrows(VerificationException.class, () -> service.sdJwtClaims(verificationResult));

        assertEquals("Failed to parse disclosure", exception.getErrorDescription());
    }

    @Test
    @DisplayName("with an unnamed array element disclosure, then it is expected to be ignored")
    void ignoresArrayElementDisclosureWithoutNamedClaim() {
        String disclosure = base64Url("[\"salt\",\"array-value\"]");
        SDJwt sdJwt = service.sdJwtFromVpToken(jwt(Map.of()) + "~" + disclosure + "~");
        when(verificationResult.getSdJwt()).thenReturn(sdJwt);

        assertEquals(Map.of(), service.sdJwtClaims(verificationResult));
    }

    @Test
    @DisplayName("with verified and unverified results, then matching statuses are expected")
    void mapsVerifiedAndUnverifiedResultsToStatus() {
        when(verificationResult.getVerified()).thenReturn(true, false);

        assertEquals(ValidationStatus.VALID, service.validationStatusSdJwt(verificationResult));
        assertEquals(ValidationStatus.INVALID, service.validationStatusSdJwt(verificationResult));
    }

    @Test
    @DisplayName("with failed verification, then observable error details are expected")
    void rejectsFailedVerificationWithObservableDetails() {
        X509Certificate certificate = certificate();
        when(mockedSdJwt.verify(any(), isNull())).thenReturn(verificationResult);
        when(verificationResult.getVerified()).thenReturn(false);
        when(verificationResult.getSignatureVerified()).thenReturn(false);
        when(verificationResult.getDisclosuresVerified()).thenReturn(true);

        VerificationException exception =
                assertThrows(VerificationException.class, () -> service.verifySdJwt(mockedSdJwt, certificate, verificationTransaction));

        assertEquals(
                "Invalid vp_token. Signature verified: false, disclosures verified: true",
                exception.getErrorDescription());
    }

    @Test
    @DisplayName("with a certificate in the compact VP token, then the issuer certificate is expected")
    void extractsCertificateFromCompactVpToken() {
        String certificate = new Certificates().getBevisportenCertificate();
        String header = """
                {"alg":"ES256","x5c":["%s"]}
                """.formatted(certificate).trim();
        SDJwt sdJwt = service.sdJwtFromVpToken(
                base64Url(header) + "." + base64Url("{}") + "." + base64Url("signature"));

        X509Certificate extracted = service.certificate(sdJwt);

        assertArrayEquals(Base64.getDecoder().decode(certificate), encoded(extracted));
    }

    @Test
    @DisplayName("without a certificate, then invalid request is expected")
    void rejectsMissingCertificate() {
        SDJwt sdJwt = service.sdJwtFromVpToken(jwt(Map.of()));

        VerificationException exception =
                assertThrows(VerificationException.class, () -> service.certificate(sdJwt));

        assertEquals("Failed to extract certificate from SDJwt", exception.getErrorDescription());
    }

    @Test
    @DisplayName("with and without a status list, then matching results are expected")
    void extractsAndOmitsStatusList() {
        SDJwt withStatus = service.sdJwtFromVpToken(jwt(Map.of(
                "status", Map.of("status_list", Map.of(
                        "idx", 7, "uri", "https://status.example/list")))));
        when(verificationResult.getSdJwt()).thenReturn(withStatus);

        StatuslistEntry status = service.extractStatuslistUriAndIdx(verificationResult);

        assertAll(
                () -> assertEquals("7", status.idx()),
                () -> assertEquals("https://status.example/list", status.uri().toString()));

        SDJwt withoutStatus = service.sdJwtFromVpToken(jwt(Map.of()));
        when(verificationResult.getSdJwt()).thenReturn(withoutStatus);
        assertNull(service.extractStatuslistUriAndIdx(verificationResult));
    }

    @Test
    @DisplayName("with an EC certificate, then matching verifier and algorithm are expected")
    void createsEcVerifierAndSelectsMatchingAlgorithm() throws Exception {
        X509Certificate certificate = certificate();

        JWSVerifier verifier = service.jwsVerifier(certificate);

        assertAll(
                () -> assertEquals(JWSAlgorithm.ES256, service.algorithm(certificate)),
                () -> assertTrue(verifier.supportedJWSAlgorithms().contains(JWSAlgorithm.ES256)));
    }

    @Test
    @DisplayName("With a valid SD-JWT, there is a cnf attributte, and the holder binding is valid")
    void checkThatThereIsACnfAttributteInSdJwt () {
        VerificationTransaction verificationTransaction = new VerificationTransaction();
        verificationTransaction.setNonce("ac93b65f-ae6c-4626-8a39-0442bfd6a0cb");
        verificationTransaction.setAudience("x509_hash:bBAVx4HHUDYrBmotXFW11s37ThLpQ_qRqRGfsAYg-8g");
        SDJwt sdjwt = service.sdJwtFromVpToken(VpTokenTestdata.VP_TOKEN);
        assertAll(
                () -> assertNotNull(sdjwt.getKeyBindingJwt()),
                () -> assertTrue(service.checkHolderBinding(verificationTransaction, sdjwt))
        );
    }

    @Test
    @DisplayName("With a valid SD-JWT, there is a cnf attributte, and the holder binding is invalid" +
            "when non-matching nonce is used")
    void nonceNotMatchingGivesFalseHolderBinding () {
        VerificationTransaction verificationTransaction = new VerificationTransaction();
        verificationTransaction.setNonce("123");
        verificationTransaction.setAudience("x509_hash:bBAVx4HHUDYrBmotXFW11s37ThLpQ_qRqRGfsAYg-8g");
        SDJwt sdjwt = service.sdJwtFromVpToken(VpTokenTestdata.VP_TOKEN);

        assertAll(
                () -> assertNotNull(sdjwt.getKeyBindingJwt()),
                () -> assertFalse(service.checkHolderBinding(verificationTransaction, sdjwt))
        );
    }

    @Test
    @DisplayName("with every validation status, then matching detail text is expected")
    void returnsValidationDetailTextForEveryStatus() {
        assertAll(
                () -> assertEquals("SD-JWT VC: SDJwt er gyldig",
                        service.getValidationDetail(ValidationStatus.VALID)),
                () -> assertEquals("SD-JWT VC: SDJwt er ugyldig",
                        service.getValidationDetail(ValidationStatus.INVALID)),
                () -> assertEquals("SD-JWT VC: validering feila",
                        service.getValidationDetail(ValidationStatus.INCONCLUSIVE)),
                () -> assertEquals("SD-JWT VC: ukjent status",
                        service.getValidationDetail(ValidationStatus.NOT_APPLICABLE)));
    }

    private static X509Certificate certificate() {
        return X509CertUtils.parse(Base64.getDecoder().decode(new Certificates().getBevisportenCertificate()));
    }

    private static byte[] encoded(X509Certificate certificate) {
        try {
            return certificate.getEncoded();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static String jwt(Map<String, Object> payload) {
        try {
            String json = JsonMapper.builder().build().writeValueAsString(payload);
            return base64Url("{\"alg\":\"ES256\"}") + "." + base64Url(json) + "." + base64Url("signature");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static String base64Url(String value) {
        return Base64URL.encode(value.getBytes(StandardCharsets.UTF_8)).toString();
    }

}
