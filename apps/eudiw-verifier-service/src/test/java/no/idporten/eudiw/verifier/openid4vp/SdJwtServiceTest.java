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
        final String vpToken = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCIsIng1YyI6WyJNSUlEQ2pDQ0FyQ2dBd0lCQWdJSUtwR0xkTlp2T1RBd0NnWUlLb1pJemowRUF3SXdnWWN4SGpBY0JnTlZCR0VURlU1VVVrNVBMVTVQUms5U0xqazVNVGd5TlRneU56RUxNQWtHQTFVRUJoTUNUazh4SkRBaUJnTlZCQW9URzBSSlIwbFVRVXhKVTBWU1NVNUhVMFJKVWtWTFZFOVNRVlJGVkRFeU1EQUdBMVVFQXhNcFpXbGtZWE15YzJGdVpHdGhjM05sSUVWQlFTQlFjbTkyYVdSbGNpQkRRU0F5SUhONWMzUmxjM1F3SGhjTk1qWXdOREk0TVRFeE1UUXdXaGNOTWpjd05ESTRNVEV4TVRRd1dqQnBNUXN3Q1FZRFZRUUdFd0pPVHpFa01DSUdBMVVFQ2d3YlJFbEhTVlJCVEVsVFJWSkpUa2RUUkVsU1JVdFVUMUpCVkVWVU1SUXdFZ1lEVlFRRERBdENaWFpwYzNCdmNuUmxiakVlTUJ3R0ExVUVZUXdWVGxSU1RrOHRUazlHVDFJdU9Ua3hPREkxT0RJM01Ga3dFd1lIS29aSXpqMENBUVlJS29aSXpqMERBUWNEUWdBRWJEVmdHK2tuenpOT3Bxa2gyejJ3eTZXQkZ5MHRzRzUzOHIyMW9nbUVGeHdRaFg0Tnlta203U2ZCVmRhUyt6YXVjdERERER0czBhNnRyNWZZTlJwWmpLT0NBU0V3Z2dFZE1COEdBMVVkSXdRWU1CYUFGS1RaSzBLK3h1VXEwdSthdFpRRlV3Z2FoSGg0TUIwR0ExVWREZ1FXQkJURjE0cnltTTZYM1ZpZnNCa3V5bTAzdGpUUDdqQU1CZ05WSFJNQkFmOEVBakFBTUZnR0ExVWRId1JSTUU4d1RhQkxvRW1HUjJoMGRIQnpPaTh2WTJFdVpXbGtZWE15YzJGdVpHdGhjM05sTG1SbGRpOTJNUzlqWlhKMGN5OXBiblJsY20xbFpHbGhkR1Z6TDJWaFlWOXdjbTkyYVdSbGNqSXVZM0pzTUdNR0NDc0dBUVVGQndFQkJGY3dWVEJUQmdnckJnRUZCUWN3QW9aSGFIUjBjSE02THk5allTNWxhV1JoY3pKellXNWthMkZ6YzJVdVpHVjJMM1l4TDJObGNuUnpMMmx1ZEdWeWJXVmthV0YwWlhNdlpXRmhYM0J5YjNacFpHVnlNaTVqWlhJd0RnWURWUjBQQVFIL0JBUURBZ1dnTUFvR0NDcUdTTTQ5QkFNQ0EwZ0FNRVVDSUNoWGgxcjRDbHgwY3YrckdDNkJaWUF5SXRXQVRVMUNsRDhJRVJ1VEM0czJBaUVBdDRtS0VUN1NYaHVNdlZZNHNoMWJaeTl5V2VnMHFHMGpuNUcvT2J3Y2Q1bz0iXX0.eyJfc2QiOlsicG8yWktrcmVwVmN4dmdXdDdLNjlscmdTQTltc2NJaWtpZ0hzYXY3aUdZZyIsImZ3bUVPQ2Z1X2QyRUE1VTdGcW5aeGg0Vzh6VHFqUUQzRVJscXhuMmJUd3MiLCJVQVktRzFoWFlPa3Y4NXp3OW1EeXl5VTBlejYxZnIwenVQcWRlVVd2dEtvIl0sInZjdCI6Im5vOmtvbnRha3RyZWdpc3RlcmV0OmtvbnRha3RpbmZvcm1hc2pvbjoxIiwiX3NkX2FsZyI6InNoYS0yNTYiLCJpc3MiOiJodHRwczovL3V0c3RlZGVyLmVpZGFzMnNhbmRrYXNzZS5kZXYvYmV2aXNnZW5lcmF0b3IiLCJjbmYiOnsiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiVDRGWVZJeS1vVVM1SWFZbHpwVE81TXZNdEVfVEhXQlFpeGtYZ2FOa0pscyIsInkiOiJpMGlMQi1iRUNBT090c1g0WWQ4Yk1VcXV1LUVqWkJQNjVTemx4a0xuRV93In19LCJleHAiOjE4MDk0MTM5NTgsImlhdCI6MTc3Nzg3Nzk1OCwic3RhdHVzIjp7InN0YXR1c19saXN0Ijp7ImlkeCI6MjY5MjQwLCJ1cmkiOiJodHRwczovL3N0YXR1cy5laWRhczJzYW5ka2Fzc2UuZGV2L2xpc3RzLzEifX19.B2S6ierAGIMMpF89DkhQDJTZWXVWXUyfZtAgHPAeN83jDFuXLsmzg0pLws2vEfkhW--dtewsSNOwVcphTqTAdQ~WyJGc2IxYm5objRUbV93aTFja2V6MnhRIiwiZXBvc3RhZHJlc3NlIiwibnVsbHN0aWx0QGFsdGlubi54eXoiXQ~WyIzU21wS2VWcEY0cG1fbGNKVkZiV0J3IiwicGVyc29uaWRlbnRpZmlrYXRvciIsIjE2OTAzMzQ5ODQ0Il0~WyIxY0lzZUpSSk85eENwZHRtMnAzSHRnIiwibW9iaWx0ZWxlZm9ubnVtbWVyIiwiKzQ3NDg5OTU4NTUiXQ~eyJ0eXAiOiJrYitqd3QiLCJhbGciOiJFUzI1NiJ9.eyJzZF9oYXNoIjoiQXl3a0pBTEdwUFQ2MC1zOElPa3BPZHpIMUpWcGNDeldZcFZLTkptd1JjNCIsImF1ZCI6Ing1MDlfaGFzaDpiQkFWeDRISFVEWXJCbW90WEZXMTFzMzdUaExwUV9xUnFSR2ZzQVlnLThnIiwibm9uY2UiOiJhYzkzYjY1Zi1hZTZjLTQ2MjYtOGEzOS0wNDQyYmZkNmEwY2IiLCJpYXQiOjE3NzkxODQ2NzF9.nm3DZGtHogv82wu22ySQiGutwGyngBB76TImnQ_MJLtQrBBDZMD673XSyuhpLiMdh6dAWxTmVyiTamS8N7uPEw";
        SDJwt sdjwt = service.sdJwtFromVpToken(vpToken);
        assertNotNull(sdjwt.getKeyBindingJwt());
        boolean test = service.checkHolderBinding(verificationTransaction, sdjwt);
        assertTrue(test);
    }

    @Test
    @DisplayName("With a valid SD-JWT, there is a cnf attributte, and the holder binding is invalid" +
            "when non-matching nonce is used")
    void nonceNotMatchingGivesFalseHolderBinding () {
        VerificationTransaction verificationTransaction = new VerificationTransaction();
        verificationTransaction.setNonce("123");
        verificationTransaction.setAudience("x509_hash:bBAVx4HHUDYrBmotXFW11s37ThLpQ_qRqRGfsAYg-8g");
        final String vpToken = "eyJhbGciOiJFUzI1NiIsInR5cCI6ImRjK3NkLWp3dCIsIng1YyI6WyJNSUlEQ2pDQ0FyQ2dBd0lCQWdJSUtwR0xkTlp2T1RBd0NnWUlLb1pJemowRUF3SXdnWWN4SGpBY0JnTlZCR0VURlU1VVVrNVBMVTVQUms5U0xqazVNVGd5TlRneU56RUxNQWtHQTFVRUJoTUNUazh4SkRBaUJnTlZCQW9URzBSSlIwbFVRVXhKVTBWU1NVNUhVMFJKVWtWTFZFOVNRVlJGVkRFeU1EQUdBMVVFQXhNcFpXbGtZWE15YzJGdVpHdGhjM05sSUVWQlFTQlFjbTkyYVdSbGNpQkRRU0F5SUhONWMzUmxjM1F3SGhjTk1qWXdOREk0TVRFeE1UUXdXaGNOTWpjd05ESTRNVEV4TVRRd1dqQnBNUXN3Q1FZRFZRUUdFd0pPVHpFa01DSUdBMVVFQ2d3YlJFbEhTVlJCVEVsVFJWSkpUa2RUUkVsU1JVdFVUMUpCVkVWVU1SUXdFZ1lEVlFRRERBdENaWFpwYzNCdmNuUmxiakVlTUJ3R0ExVUVZUXdWVGxSU1RrOHRUazlHVDFJdU9Ua3hPREkxT0RJM01Ga3dFd1lIS29aSXpqMENBUVlJS29aSXpqMERBUWNEUWdBRWJEVmdHK2tuenpOT3Bxa2gyejJ3eTZXQkZ5MHRzRzUzOHIyMW9nbUVGeHdRaFg0Tnlta203U2ZCVmRhUyt6YXVjdERERER0czBhNnRyNWZZTlJwWmpLT0NBU0V3Z2dFZE1COEdBMVVkSXdRWU1CYUFGS1RaSzBLK3h1VXEwdSthdFpRRlV3Z2FoSGg0TUIwR0ExVWREZ1FXQkJURjE0cnltTTZYM1ZpZnNCa3V5bTAzdGpUUDdqQU1CZ05WSFJNQkFmOEVBakFBTUZnR0ExVWRId1JSTUU4d1RhQkxvRW1HUjJoMGRIQnpPaTh2WTJFdVpXbGtZWE15YzJGdVpHdGhjM05sTG1SbGRpOTJNUzlqWlhKMGN5OXBiblJsY20xbFpHbGhkR1Z6TDJWaFlWOXdjbTkyYVdSbGNqSXVZM0pzTUdNR0NDc0dBUVVGQndFQkJGY3dWVEJUQmdnckJnRUZCUWN3QW9aSGFIUjBjSE02THk5allTNWxhV1JoY3pKellXNWthMkZ6YzJVdVpHVjJMM1l4TDJObGNuUnpMMmx1ZEdWeWJXVmthV0YwWlhNdlpXRmhYM0J5YjNacFpHVnlNaTVqWlhJd0RnWURWUjBQQVFIL0JBUURBZ1dnTUFvR0NDcUdTTTQ5QkFNQ0EwZ0FNRVVDSUNoWGgxcjRDbHgwY3YrckdDNkJaWUF5SXRXQVRVMUNsRDhJRVJ1VEM0czJBaUVBdDRtS0VUN1NYaHVNdlZZNHNoMWJaeTl5V2VnMHFHMGpuNUcvT2J3Y2Q1bz0iXX0.eyJfc2QiOlsicG8yWktrcmVwVmN4dmdXdDdLNjlscmdTQTltc2NJaWtpZ0hzYXY3aUdZZyIsImZ3bUVPQ2Z1X2QyRUE1VTdGcW5aeGg0Vzh6VHFqUUQzRVJscXhuMmJUd3MiLCJVQVktRzFoWFlPa3Y4NXp3OW1EeXl5VTBlejYxZnIwenVQcWRlVVd2dEtvIl0sInZjdCI6Im5vOmtvbnRha3RyZWdpc3RlcmV0OmtvbnRha3RpbmZvcm1hc2pvbjoxIiwiX3NkX2FsZyI6InNoYS0yNTYiLCJpc3MiOiJodHRwczovL3V0c3RlZGVyLmVpZGFzMnNhbmRrYXNzZS5kZXYvYmV2aXNnZW5lcmF0b3IiLCJjbmYiOnsiandrIjp7Imt0eSI6IkVDIiwiY3J2IjoiUC0yNTYiLCJ4IjoiVDRGWVZJeS1vVVM1SWFZbHpwVE81TXZNdEVfVEhXQlFpeGtYZ2FOa0pscyIsInkiOiJpMGlMQi1iRUNBT090c1g0WWQ4Yk1VcXV1LUVqWkJQNjVTemx4a0xuRV93In19LCJleHAiOjE4MDk0MTM5NTgsImlhdCI6MTc3Nzg3Nzk1OCwic3RhdHVzIjp7InN0YXR1c19saXN0Ijp7ImlkeCI6MjY5MjQwLCJ1cmkiOiJodHRwczovL3N0YXR1cy5laWRhczJzYW5ka2Fzc2UuZGV2L2xpc3RzLzEifX19.B2S6ierAGIMMpF89DkhQDJTZWXVWXUyfZtAgHPAeN83jDFuXLsmzg0pLws2vEfkhW--dtewsSNOwVcphTqTAdQ~WyJGc2IxYm5objRUbV93aTFja2V6MnhRIiwiZXBvc3RhZHJlc3NlIiwibnVsbHN0aWx0QGFsdGlubi54eXoiXQ~WyIzU21wS2VWcEY0cG1fbGNKVkZiV0J3IiwicGVyc29uaWRlbnRpZmlrYXRvciIsIjE2OTAzMzQ5ODQ0Il0~WyIxY0lzZUpSSk85eENwZHRtMnAzSHRnIiwibW9iaWx0ZWxlZm9ubnVtbWVyIiwiKzQ3NDg5OTU4NTUiXQ~eyJ0eXAiOiJrYitqd3QiLCJhbGciOiJFUzI1NiJ9.eyJzZF9oYXNoIjoiQXl3a0pBTEdwUFQ2MC1zOElPa3BPZHpIMUpWcGNDeldZcFZLTkptd1JjNCIsImF1ZCI6Ing1MDlfaGFzaDpiQkFWeDRISFVEWXJCbW90WEZXMTFzMzdUaExwUV9xUnFSR2ZzQVlnLThnIiwibm9uY2UiOiJhYzkzYjY1Zi1hZTZjLTQ2MjYtOGEzOS0wNDQyYmZkNmEwY2IiLCJpYXQiOjE3NzkxODQ2NzF9.nm3DZGtHogv82wu22ySQiGutwGyngBB76TImnQ_MJLtQrBBDZMD673XSyuhpLiMdh6dAWxTmVyiTamS8N7uPEw";
        SDJwt sdjwt = service.sdJwtFromVpToken(vpToken);
        assertNotNull(sdjwt.getKeyBindingJwt());
        boolean test = service.checkHolderBinding(verificationTransaction, sdjwt);
        assertFalse(test);
    }




//    @Test
//    @DisplayName("when holder binding is required, then dcql contains cnf")
//    void detectsHolderBindingRequired() {
//        assertTrue(service.holderBindingRequired("$.cnf"));
//        assertTrue(service.holderBindingRequired("$.iss, $.cnf, $.sub"));
//        assertFalse(service.holderBindingRequired("$.iss, $.sub"));
//    }
//
//    @Test
//    @DisplayName("with a valid EC JWK in cnf, then holder binding succeeds")
//    void acceptsValidHolderBinding() throws Exception {
//        VerificationTransaction tx = new VerificationTransaction();
//        tx.setAudience("https://verifier.example");
//        tx.setNonce("nonce-123");
//
//        Map rawPayload = new java.util.HashMap();
//        Map<String, Object> cnf = new java.util.HashMap<>();
//        cnf.put("jwk", validEcJwk());
//        rawPayload.put("cnf", cnf);
//
//        SDJwt mockedSdJwt = mock(SDJwt.class);
//        id.walt.sdjwt.KeyBindingJwt keyBindingJwt = mock(id.walt.sdjwt.KeyBindingJwt.class);
//
//        when(mockedSdJwt.getFullPayload()).thenReturn((JsonObject) rawPayload);
//        when(mockedSdJwt.getKeyBindingJwt()).thenReturn(keyBindingJwt);
//        when(keyBindingJwt.verifyKB(any(), eq("https://verifier.example"), eq("nonce-123"), eq(mockedSdJwt), isNull()))
//                .thenReturn(true);
//
//        assertTrue(service.checkHolderBinding(tx, mockedSdJwt));
//    }
//
//    @Test
//    @DisplayName("with mismatched nonce, then holder binding fails")
//    void rejectsInvalidNonce() throws Exception {
//        VerificationTransaction tx = new VerificationTransaction();
//        tx.setAudience("https://verifier.example");
//        tx.setNonce("correct-nonce");
//
//        Map rawPayload = new java.util.HashMap();
//        Map<String, Object> cnf = new java.util.HashMap<>();
//        cnf.put("jwk", validEcJwk());
//        rawPayload.put("cnf", cnf);
//
//        SDJwt mockedSdJwt = mock(SDJwt.class);
//        id.walt.sdjwt.KeyBindingJwt keyBindingJwt = mock(id.walt.sdjwt.KeyBindingJwt.class);
//
//        when(mockedSdJwt.getFullPayload()).thenReturn((JsonObject) rawPayload);
//        when(mockedSdJwt.getKeyBindingJwt()).thenReturn(keyBindingJwt);
//        when(keyBindingJwt.verifyKB(any(), eq("https://verifier.example"), eq("correct-nonce"), eq(mockedSdJwt), isNull()))
//                .thenReturn(false);
//
//        assertFalse(service.checkHolderBinding(tx, mockedSdJwt));
//    }
//
//    @Test
//    @DisplayName("with mismatched audience, then holder binding fails")
//    void rejectsInvalidAudience() throws Exception {
//        VerificationTransaction tx = new VerificationTransaction();
//        tx.setAudience("https://wrong-verifier.example");
//        tx.setNonce("nonce-123");
//
//        Map rawPayload = new java.util.HashMap();
//        Map<String, Object> cnf = new java.util.HashMap<>();
//        cnf.put("jwk", validEcJwk());
//        rawPayload.put("cnf", cnf);
//
//        SDJwt mockedSdJwt = mock(SDJwt.class);
//        id.walt.sdjwt.KeyBindingJwt keyBindingJwt = mock(id.walt.sdjwt.KeyBindingJwt.class);
//
//        when(mockedSdJwt.getFullPayload()).thenReturn((JsonObject) rawPayload);
//        when(mockedSdJwt.getKeyBindingJwt()).thenReturn(keyBindingJwt);
//        when(keyBindingJwt.verifyKB(any(), eq("https://wrong-verifier.example"), eq("nonce-123"), eq(mockedSdJwt), isNull()))
//                .thenReturn(false);
//
//        assertFalse(service.checkHolderBinding(tx, mockedSdJwt));
//    }
//
//    @Test
//    @DisplayName("with missing cnf, then holder binding throws exception")
//    void rejectsMissingCnf() {
//        VerificationTransaction tx = new VerificationTransaction();
//        tx.setAudience("https://verifier.example");
//        tx.setNonce("nonce-123");
//
//        SDJwt mockedSdJwt = mock(SDJwt.class);
//        Map rawPayload = new java.util.HashMap();
//        when(mockedSdJwt.getFullPayload()).thenReturn((JsonObject) rawPayload);
//
//        assertThrows(NullPointerException.class, () -> service.checkHolderBinding(tx, mockedSdJwt));
//    }
//
//    private static Map<String, Object> validEcJwk() throws Exception {
//        com.nimbusds.jose.jwk.ECKey ecKey = new com.nimbusds.jose.jwk.gen.ECKeyGenerator(com.nimbusds.jose.jwk.Curve.P_256)
//                .keyID("holder-key")
//                .generate();
//
//        Map<String, Object> jwk = new java.util.LinkedHashMap<>();
//        jwk.put("kty", ecKey.getKeyType().toString());
//        jwk.put("crv", ecKey.getCurve().getName());
//        jwk.put("kid", ecKey.getKeyID());
//        jwk.put("x", ecKey.getX().toString());
//        jwk.put("y", ecKey.getY().toString());
//
//        return jwk;
//    }

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
