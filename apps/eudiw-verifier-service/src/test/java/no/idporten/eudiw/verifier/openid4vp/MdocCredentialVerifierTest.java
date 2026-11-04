package no.idporten.eudiw.verifier.openid4vp;

import com.upokecenter.cbor.CBORObject;
import id.walt.cose.CoseInternalKt;
import id.walt.policies2.vp.policies.*;
import kotlinx.serialization.cbor.CborElement;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.walt.WaltPresentations;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static no.idporten.eudiw.verifier.openid4vp.VerifierTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("When verifying an mdoc presentation")
class MdocCredentialVerifierTest {

    private static final String EXPIRED = "mdoc/launchpad/lu/0120561d-6648-4767-8466-1d5dd6b1163f.mdoc";

    private final MdocCredentialVerifier verifier = new MdocCredentialVerifier();

    @Test
    @DisplayName("with an expired MSO, then invalid status is expected")
    void rejectsExpiredMso() throws Exception {
        CredentialVerificationResult verification = verifier.verify(read(EXPIRED), mdocQuery(false), new VerificationTransaction());

        assertAll(
                () -> assertEquals(ValidationStatus.INVALID, verification.credentialStatus()),
                () -> assertEquals(ValidationStatus.NOT_APPLICABLE, verification.holderBindingStatus()),
                () -> assertNotNull(verification.issuerCertificate()));
    }

    @Test
    @DisplayName("with holder binding required and a policy list without device authentication, then holder binding failure is expected")
    void requiresDeviceAuthenticationPolicyForHolderBinding() throws Exception {
        String vpToken = read(EXPIRED);
        MdocCredentialVerifier withoutDeviceAuth = new MdocCredentialVerifier(Clock.systemUTC(), List.of(new IssuerAuthMdocVpPolicy()));

        assertHolderBindingFails(() -> withoutDeviceAuth.verify(vpToken, mdocQuery(true), sessionOf(EXPIRED)));
    }

    @Test
    @DisplayName("with no, two or malformed documents, then invalid request is expected")
    void rejectsUnsupportedDeviceResponses() throws Exception {
        CBORObject deviceResponse = CBORObject.DecodeFromBytes(Base64.getUrlDecoder().decode(read(EXPIRED)));
        deviceResponse.get("documents").Add(deviceResponse.get("documents").get(0));
        String twoDocuments = Base64.getUrlEncoder().withoutPadding().encodeToString(deviceResponse.EncodeToBytes());

        assertAll(
                () -> assertRejected("o2d2ZXJzaW9uYzEuMGlkb2N1bWVudHOAZnN0YXR1cwA", "No mdoc documents in vp_token"),
                () -> assertRejected(twoDocuments, "Multiple mdoc documents in vp_token, only one is supported"),
                () -> assertRejected("not-an-mdoc", "Failed to parse mdoc vp_token"));
    }

    @Test
    @DisplayName("with supported data element values, then matching Java values are expected")
    void convertsSupportedDataElementValues() {
        CBORObject map = CBORObject.NewMap().Add("flag", true).Add("name", "Ada");

        assertAll(
                () -> assertEquals(true, MdocCredentialVerifier.cborValue(cbor(CBORObject.True))),
                () -> assertEquals(42L, MdocCredentialVerifier.cborValue(cbor(CBORObject.FromObject(42)))),
                () -> assertEquals(-42L, MdocCredentialVerifier.cborValue(cbor(CBORObject.FromObject(-42)))),
                () -> assertEquals("Ada", MdocCredentialVerifier.cborValue(cbor(CBORObject.FromObject("Ada")))),
                () -> assertEquals("1922-06-22", MdocCredentialVerifier.cborValue(cbor(CBORObject.FromObjectAndTag("1922-06-22", 1004)))),
                () -> assertEquals(List.of("Ada", 42L), MdocCredentialVerifier.cborValue(cbor(CBORObject.NewArray().Add("Ada").Add(42)))),
                () -> assertEquals(Map.of("flag", true, "name", "Ada"), MdocCredentialVerifier.cborValue(cbor(map))),
                () -> assertEquals(Base64.getEncoder().encodeToString("bytes".getBytes(StandardCharsets.UTF_8)),
                        MdocCredentialVerifier.cborValue(cbor(CBORObject.FromObject("bytes".getBytes(StandardCharsets.UTF_8))))),
                () -> assertNull(MdocCredentialVerifier.cborValue(cbor(CBORObject.Null))),
                () -> assertNull(MdocCredentialVerifier.cborValue(null)));
    }

    private void assertRejected(String vpToken, String message) {
        VerificationException exception = assertThrows(VerificationException.class,
                () -> verifier.verify(vpToken, mdocQuery(true), new VerificationTransaction()));
        assertEquals(message, exception.getErrorDescription());
    }

    private static CborElement cbor(CBORObject value) {
        return CoseInternalKt.getCoseCompliantCbor()
                .decodeFromByteArray(CborElement.Companion.serializer(), value.EncodeToBytes());
    }

    /**
     * The validity period of the MSO is checked by a policy against the system clock, so it is left out. The issuer
     * certificate is checked at the MSO validFrom time.
     */
    @Nested
    @DisplayName("with PIDs from the EUDI launchpad")
    class Launchpad {

        // ISO/IEC 18013-5 does not allow fractions of seconds in MSO timestamps. Only the Portuguese issuer does this,
        // so it is not worked around.
        private static final String FRACTIONAL_SECONDS = "mdoc/launchpad/pt/";
        // clr and oidf are conformance test suites that include negative tests, so some of their samples should fail.
        // MSO is signed before the document signer certificate is valid
        private static final String SIGNED_BEFORE_CERTIFICATE = "mdoc/launchpad/clr/b7bee650-587a-4837-a283-948877ae8dde.mdoc";
        // Negative conformance tests: the device signature is invalid
        private static final Set<String> INVALID_HOLDER_BINDING = Set.of(
                "mdoc/launchpad/clr/c45978fb-df09-4084-b063-3fd6e0db2668.mdoc",
                "mdoc/launchpad/oidf/54dcc7e9-109d-4a84-8a8a-0a8c0fc99cdc.mdoc");

        private static final List<MdocVPPolicy> POLICIES_WITHOUT_VALIDITY_PERIOD = List.of(
                new DeviceAuthMdocVpPolicy(), new IssuerAuthMdocVpPolicy(), new IssuerSignedDataMdocVpPolicy());

        static Stream<String> validSamples() throws Exception {
            return samples("mdoc/launchpad", ".mdoc").filter(sample -> !sample.startsWith(FRACTIONAL_SECONDS)
                    && !sample.equals(SIGNED_BEFORE_CERTIFICATE) && !INVALID_HOLDER_BINDING.contains(sample));
        }

        static Stream<String> fractionalSecondsSamples() throws Exception {
            return samples("mdoc/launchpad", ".mdoc").filter(sample -> sample.startsWith(FRACTIONAL_SECONDS));
        }

        static Stream<String> invalidHolderBinding() {
            return INVALID_HOLDER_BINDING.stream().sorted();
        }

        @DisplayName("with a valid mdoc, then PID claims, issuer certificate and valid statuses are expected")
        @ParameterizedTest(name = "{0}")
        @MethodSource("validSamples")
        void verifiesMdoc(String sample) throws Exception {
            String vpToken = read(sample);

            CredentialVerificationResult verification = launchpadVerifier(clockAt(msoValidFrom(vpToken)))
                    .verify(vpToken, mdocQuery(true), sessionOf(sample));

            assertAll(
                    () -> assertEquals(CredentialFormat.MSO_MDOC, verification.format()),
                    () -> assertEquals(PID_DOC_TYPE, verification.credentialType()),
                    () -> assertInstanceOf(Map.class, verification.claims().get(PID_DOC_TYPE)),
                    () -> assertNotNull(verification.issuerCertificate()),
                    () -> assertEquals(ValidationStatus.VALID, verification.credentialStatus()),
                    () -> assertEquals(ValidationStatus.VALID, verification.holderBindingStatus()));
        }

        @DisplayName("with a valid mdoc and the session data of another presentation, then holder binding failure is expected")
        @ParameterizedTest(name = "{0}")
        @MethodSource("validSamples")
        void rejectsMdocForAnotherSession(String sample) throws Exception {
            String vpToken = read(sample);
            VerificationTransaction transaction = sessionOf(sample);
            transaction.setNonce("another-nonce");

            assertHolderBindingFails(() -> launchpadVerifier(clockAt(msoValidFrom(vpToken))).verify(vpToken, mdocQuery(true), transaction));
        }

        @DisplayName("with invalid holder binding, then holder binding failure is expected")
        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidHolderBinding")
        void rejectsInvalidHolderBinding(String sample) throws Exception {
            String vpToken = read(sample);

            assertHolderBindingFails(() -> launchpadVerifier(clockAt(msoValidFrom(vpToken))).verify(vpToken, mdocQuery(true), sessionOf(sample)));
        }

        @DisplayName("with fractions of seconds in the MSO, then invalid status without issuer certificate is expected")
        @ParameterizedTest(name = "{0}")
        @MethodSource("fractionalSecondsSamples")
        void rejectsFractionalSecondsInMso(String sample) throws Exception {
            String vpToken = read(sample);
            MdocCredentialVerifier launchpadVerifier = launchpadVerifier(Clock.systemUTC());

            CredentialVerificationResult verification = launchpadVerifier.verify(vpToken, mdocQuery(false), sessionOf(sample));

            assertAll(
                    () -> assertEquals(ValidationStatus.INVALID, verification.credentialStatus()),
                    () -> assertEquals(ValidationStatus.NOT_APPLICABLE, verification.holderBindingStatus()),
                    () -> assertNull(verification.issuerCertificate()),
                    () -> assertHolderBindingFails(() -> launchpadVerifier.verify(vpToken, mdocQuery(true), sessionOf(sample))));
        }

        @Test
        @DisplayName("with an MSO signed before the issuer certificate is valid, then invalid status is expected")
        void rejectsMsoSignedBeforeCertificateValidity() throws Exception {
            String vpToken = read(SIGNED_BEFORE_CERTIFICATE);

            CredentialVerificationResult verification = launchpadVerifier(clockAt(msoValidFrom(vpToken)))
                    .verify(vpToken, mdocQuery(true), sessionOf(SIGNED_BEFORE_CERTIFICATE));

            assertAll(
                    () -> assertEquals(ValidationStatus.INVALID, verification.credentialStatus()),
                    () -> assertEquals(ValidationStatus.VALID, verification.holderBindingStatus()));
        }

        private static MdocCredentialVerifier launchpadVerifier(Clock clock) {
            return new MdocCredentialVerifier(clock, POLICIES_WITHOUT_VALIDITY_PERIOD);
        }

        private static Instant msoValidFrom(String vpToken) {
            return Instant.ofEpochSecond(WaltPresentations.parseMdocPresentation(vpToken)
                    .getMdoc().getDocumentMso().getValidityInfo().getValidFrom().getEpochSeconds());
        }
    }
}
