package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jwt.SignedJWT;
import id.walt.policies2.vp.policies.*;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import no.idporten.eudiw.verifier.testdata.VpTokenTestdata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static no.idporten.eudiw.verifier.openid4vp.VerifierTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When verifying an SD-JWT VC presentation")
class SdJwtCredentialVerifierTest {

    // VpTokenTestdata.VP_TOKEN: session data from the authorization request and the iat of its key binding JWT
    private static final String NONCE = "ac93b65f-ae6c-4626-8a39-0442bfd6a0cb";
    private static final String AUDIENCE = "x509_hash:bBAVx4HHUDYrBmotXFW11s37ThLpQ_qRqRGfsAYg-8g";
    private static final Instant KB_JWT_ISSUED_AT = Instant.ofEpochSecond(1779184671);

    private final SdJwtCredentialVerifier verifier = verifierAt(KB_JWT_ISSUED_AT);

    @Test
    @DisplayName("with valid holder binding, then claims, issuer certificate, status and valid statuses are expected")
    void verifiesPresentation() {
        CredentialVerification verification = verifier.verify(VpTokenTestdata.VP_TOKEN, sdJwtQuery(true), transaction(NONCE, AUDIENCE));

        assertAll(
                () -> assertEquals(CredentialFormat.DC_SD_JWT, verification.format()),
                () -> assertEquals("no:kontaktregisteret:kontaktinformasjon:1", verification.credentialType()),
                () -> assertFalse(verification.claims().isEmpty()),
                () -> assertNotNull(verification.issuerCertificate()),
                () -> assertEquals(new StatuslistEntry("269240", URI.create("https://status.eidas2sandkasse.dev/lists/1")),
                        verification.statuslistEntry()),
                () -> assertEquals(ValidationStatus.VALID, verification.credentialStatus()),
                () -> assertEquals(ValidationStatus.VALID, verification.holderBindingStatus()));
    }

    @Test
    @DisplayName("with another nonce, audience or a stale key binding JWT, then holder binding failure is expected")
    void rejectsHolderBindingForAnotherSession() {
        SdJwtCredentialVerifier later = verifierAt(KB_JWT_ISSUED_AT.plus(Duration.ofHours(1)));

        assertAll(
                () -> assertHolderBindingFails(() -> verifier.verify(VpTokenTestdata.VP_TOKEN, sdJwtQuery(true), transaction("another-nonce", AUDIENCE))),
                () -> assertHolderBindingFails(() -> verifier.verify(VpTokenTestdata.VP_TOKEN, sdJwtQuery(true), transaction(NONCE, "x509_hash:another"))),
                () -> assertHolderBindingFails(() -> later.verify(VpTokenTestdata.VP_TOKEN, sdJwtQuery(true), transaction(NONCE, AUDIENCE))));
    }

    @Test
    @DisplayName("without key binding JWT, then holder binding failure is expected only when holder binding is required")
    void requiresKeyBindingJwtOnlyWhenHolderBindingIsRequired() {
        String withoutKeyBinding = VpTokenTestdata.VP_TOKEN.substring(0, VpTokenTestdata.VP_TOKEN.lastIndexOf('~') + 1);

        CredentialVerification verification = verifier.verify(withoutKeyBinding, sdJwtQuery(false), transaction(NONCE, AUDIENCE));

        assertAll(
                () -> assertHolderBindingFails(() -> verifier.verify(withoutKeyBinding, sdJwtQuery(true), transaction(NONCE, AUDIENCE))),
                () -> assertEquals(ValidationStatus.VALID, verification.credentialStatus()),
                () -> assertFalse(verification.claims().isEmpty()));
    }

    @Test
    @DisplayName("with a modified issuer signature, then invalid request is expected")
    void rejectsInvalidIssuerSignature() {
        String[] parts = VpTokenTestdata.VP_TOKEN.split("~", 2);
        String signature = parts[0].substring(parts[0].lastIndexOf('.') + 1);
        String modified = parts[0].substring(0, parts[0].lastIndexOf('.') + 1)
                + (signature.charAt(0) == 'A' ? 'B' : 'A') + signature.substring(1) + "~" + parts[1];

        VerificationException exception = assertThrows(VerificationException.class,
                () -> verifier.verify(modified, sdJwtQuery(true), transaction(NONCE, AUDIENCE)));

        assertEquals("Invalid vp_token. Issuer signature is invalid.", exception.getErrorDescription());
    }

    @Test
    @DisplayName("without x5c in the header, then invalid request is expected, since the issuer key is not resolved otherwise")
    void requiresX5c() {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"ES256\",\"typ\":\"dc+sd-jwt\"}".getBytes(StandardCharsets.UTF_8));
        String withoutX5c = header + VpTokenTestdata.VP_TOKEN.substring(VpTokenTestdata.VP_TOKEN.indexOf('.'));

        VerificationException exception = assertThrows(VerificationException.class,
                () -> verifier.verify(withoutX5c, sdJwtQuery(true), transaction(NONCE, AUDIENCE)));

        assertEquals("Failed to extract certificate from SDJwt or certificate is null", exception.getErrorDescription());
    }

    private static SdJwtCredentialVerifier verifierAt(Instant instant) {
        return new SdJwtCredentialVerifier(clockAt(instant), SdJwtCredentialVerifier.defaultPolicies(Duration.ofSeconds(60)));
    }

    /**
     * The validity period is checked by policies against the system clock, so it is left out. The age of the key
     * binding JWT is checked at its own iat.
     */
    @Nested
    @DisplayName("with PIDs from the EUDI launchpad")
    class Launchpad {

        // Negative conformance test: the key binding JWT signature is invalid
        private static final Set<String> INVALID_HOLDER_BINDING = Set.of(
                "sdjwt/launchpad/oidf/0011ed29-8e5d-44f3-ac5c-dd3320596ce8.sdjwt");

        private static final List<DcSdJwtVPPolicy> POLICIES_WITHOUT_VALIDITY_PERIOD = List.of(
                new KbJwtSignatureSdJwtVPPolicy(), new NonceCheckSdJwtVPPolicy(), new AudienceCheckSdJwtVPPolicy(),
                new SdHashCheckSdJwtVPPolicy(), new KbJwtIatCheckSdJwtVPPolicy());

        static Stream<String> validSamples() throws Exception {
            return samples("sdjwt/launchpad", ".sdjwt").filter(sample -> !INVALID_HOLDER_BINDING.contains(sample));
        }

        static Stream<String> invalidHolderBinding() {
            return INVALID_HOLDER_BINDING.stream().sorted();
        }

        @DisplayName("with a valid SD-JWT VC, then PID claims, issuer certificate and valid statuses are expected")
        @ParameterizedTest(name = "{0}")
        @MethodSource("validSamples")
        void verifiesSdJwt(String sample) throws Exception {
            String vpToken = read(sample);

            CredentialVerification verification = launchpadVerifier(vpToken).verify(vpToken, sdJwtQuery(true), sessionOf(sample));

            assertAll(
                    () -> assertTrue(verification.credentialType().startsWith("urn:eudi:pid:")),
                    () -> assertFalse(verification.claims().isEmpty()),
                    () -> assertNotNull(verification.issuerCertificate()),
                    () -> assertEquals(ValidationStatus.VALID, verification.credentialStatus()),
                    () -> assertEquals(ValidationStatus.VALID, verification.holderBindingStatus()));
        }

        @DisplayName("with a valid SD-JWT VC and the session data of another presentation, then holder binding failure is expected")
        @ParameterizedTest(name = "{0}")
        @MethodSource("validSamples")
        void rejectsSdJwtForAnotherSession(String sample) throws Exception {
            String vpToken = read(sample);
            VerificationTransaction transaction = sessionOf(sample);
            transaction.setNonce("another-nonce");

            assertHolderBindingFails(() -> launchpadVerifier(vpToken).verify(vpToken, sdJwtQuery(true), transaction));
        }

        @DisplayName("with invalid holder binding, then holder binding failure is expected")
        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidHolderBinding")
        void rejectsInvalidHolderBinding(String sample) throws Exception {
            String vpToken = read(sample);

            assertHolderBindingFails(() -> launchpadVerifier(vpToken).verify(vpToken, sdJwtQuery(true), sessionOf(sample)));
        }

        private static SdJwtCredentialVerifier launchpadVerifier(String vpToken) throws ParseException {
            Instant keyBindingIssuedAt = SignedJWT.parse(vpToken.substring(vpToken.lastIndexOf('~') + 1))
                    .getJWTClaimsSet().getIssueTime().toInstant();
            return new SdJwtCredentialVerifier(clockAt(keyBindingIssuedAt), POLICIES_WITHOUT_VALIDITY_PERIOD);
        }
    }
}
