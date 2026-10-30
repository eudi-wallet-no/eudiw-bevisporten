package no.idporten.eudiw.verifier.statuslist;

import id.walt.mdoc.cose.COSESign1;
import id.walt.mdoc.dataelement.ByteStringElement;
import id.walt.mdoc.dataelement.MapElement;
import id.walt.mdoc.dataelement.MapKey;
import id.walt.mdoc.dataelement.NumberElement;
import id.walt.mdoc.dataelement.StringElement;
import no.idporten.eudiw.verifier.VerificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

import static java.time.temporal.ChronoUnit.NANOS;
import static no.idporten.eudiw.verifier.statuslist.StatusListTestUtil.createSignedStatusListCwtWithRsa;
import static no.idporten.eudiw.verifier.statuslist.StatusListCwtValidator.CWT_ISSUED_AT_CLAIM;
import static no.idporten.eudiw.verifier.statuslist.StatusListCwtValidator.CWT_STATUS_LIST_CLAIM;
import static no.idporten.eudiw.verifier.statuslist.StatusListCwtValidator.CWT_SUBJECT_CLAIM;
import static no.idporten.eudiw.verifier.statuslist.StatusListCwtValidator.STATUS_LIST_BITS;
import static no.idporten.eudiw.verifier.statuslist.StatusListCwtValidator.STATUS_LIST_DATA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DisplayName("When validating and resolving status from status list CWT")
class StatusListCwtValidatorTest {
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);
    private static final Instant FIXED_NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final URI STATUS_LIST_URI = URI.create("https://status.example/list/1");

    private static final String RFC_CWT_STATUS_LIST_HEX = """
            d2845820a2012610781a6170706c69636174696f6e2f7374617475736c6973742b63
            7774a1044231325850a502782168747470733a2f2f6578616d706c652e636f6d2f73
            74617475736c697374732f31061a648c5bea041a8898dfea19fffe19a8c019fffda2
            646269747301636c73744a78dadbb918000217015d584093fa4d01032b18c35e2fe1
            101b77fd6cc9440022caa4694450c4e4e9feab4e99d1fa6d9772ce2bf3a12e0323de
            d7c982c5e101a5e67f0cbc1e2b6f57ce99c279
            """;

    private static final String DIIA_CWT_BASE64 = "0oRYIKIBJhB4GmFwcGxpY2F0aW9uL3N0YXR1c2xpc3QrY3d0oRghWQL3MIIC8zCCApigAwIBAgIQAbc0bwF/8IesXS/NjpEnajAKBggqhkjOPQQDAjBBMQswCQYDVQQGEwJVQTEUMBIGA1UECgwLRGlpYSAodGVzdCkxHDAaBgNVBAMME0RpaWEgU2VhbCBDQSAodGVzdCkwHhcNMjYxMDEzMTIyMDQ5WhcNMjcxMDEzMTIyMDQ5WjBgMQswCQYDVQQGEwJVQTEUMBIGA1UECgwLRGlpYSAodGVzdCkxFzAVBgNVBGEMDk5UUlVBLTAwMDAwMDAwMSIwIAYDVQQDDBlEaWlhIFN0YXR1cyBTaWduZXIgKHRlc3QpMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAElALp0H+fs4KB7rDyBG07CwvE07Pe01yyr/no2UMLkOgr3k/8QKJQufs0k2ACovchRC956+e4auNCjXb8yoz2z6OCAVEwggFNMAwGA1UdEwEB/wQCMAAwDgYDVR0PAQH/BAQDAgeAMB0GA1UdDgQWBBQKn2IpTffnMNrNstKYGx1jm2ELITAfBgNVHSMEGDAWgBQIuQCJ6LwbSCGdJxnDmYQUtmfljTBRBggrBgEFBQcBAQRFMEMwQQYIKwYBBQUHMAKGNWh0dHBzOi8vZGlpYS1pc3N1ZXItYXBpMnMuZGlpYS5nb3YudWEvcGtpL3NlYWwtY2EuY2VyMEYGA1UdHwQ/MD0wO6A5oDeGNWh0dHBzOi8vZGlpYS1pc3N1ZXItYXBpMnMuZGlpYS5nb3YudWEvcGtpL3NlYWwtY2EuY3JsMFIGA1UdIARLMEkwRwYDiDcBMEAwPgYIKwYBBQUHAgEWMmh0dHBzOi8vZGlpYS1pc3N1ZXItYXBpMnMuZGlpYS5nb3YudWEvcGtpL2Nwcy5odG1sMAoGCCqGSM49BAMCA0kAMEYCIQDFfDFhvmY5Ey3Ncph+tT0Pm2K8IsdSYYldFdHD6wfihQIhAIgSqLt0fOCs84xyWHJHQ0wj2gMp0W7pH00KYtL9WIYUWQHKpQF4LGh0dHBzOi8vYXBpMnMuZGlpYS5nb3YudWEvYXBpL3YxL2V1aWQvd2FsbGV0Anh+aHR0cHM6Ly9hcGkycy5kaWlhLmdvdi51YS9hcGkvdjEvZXVpZC93YWxsZXQvdG9rZW5fc3RhdHVzX2xpc3QvVUEvZXUuZXVyb3BhLmVjLmV1ZGkucGlkLjEvOTVkMTk4OWUtOTMxOC00NmMzLTk0NDUtNjVlMzRhNjY4N2RjBBpq5Y68Bhpq5D0AGf/9omNsc3RY/Xja7ZpLDsMwCEQTy4sscwQfxUfz0dt1F23zIQz4PSmrSDXCzABRl0WbzeHM+uN9k4kEXK4FZOhWP1zez4ocEt3o//dOE/SnhD6zURoJLGUldQyH5AlYxyAagxTk5Y6xD6sHO/bc4lFaW0EGNkaDNRy0PQ+BoOIbi7TrBQZwkBooyKlMeVf0i3PtmQ3+OcrZMpNh+BzbLkVVnsg6OoKPhp1gmDR2oPFn02oP6OvaARnHn+EdwC0fHCJ9Ta1CeUuqhipSFWPJwjaNIULIpSW2lwKo0LMMWuBcLA6YbZDT/aUzbotvyBLmYMio1MQfe9p7O5rHYmdqX0voBeZ8CkNkYml0cwFYQPRsEcCaQqq5QaV2J1crVave94pU7zIS8ZUuBzy7eCWUCZJkC1eyGV2yfFqMIKCY0gr65rJV1hs7bgJtV7elJxY=";

    @Test
    @DisplayName("and x5chain is a single byte string in the unprotected header then signature should be verified and status resolved")
    void acceptsSingleCertificateX5ChainInUnprotectedHeader() {
        StatusListCwtValidator validator = new StatusListCwtValidator(CLOCK_SKEW);
        byte[] cwt = java.util.Base64.getDecoder().decode(DIIA_CWT_BASE64);
        URI uri = URI.create("https://api2s.diia.gov.ua/api/v1/euid/wallet/token_status_list/UA/eu.europa.ec.eudi.pid.1/95d1989e-9318-46c3-9445-65e34a6687dc");

        int status = validator.validateAndResolveStatus(uri, 0, cwt, Instant.ofEpochSecond(0x6ae43d00L + 60));

        assertEquals(0, status);
    }

    @Test
    @DisplayName("and the RFC CWT example is parsed then status-list claims should be resolved")
    void parsesRfcCwtExampleAndResolvesStatus() {
        StatusListCwtValidator validator = new StatusListCwtValidator(Duration.ofSeconds(60));
        byte[] cwtBytes = HexFormat.of().parseHex(RFC_CWT_STATUS_LIST_HEX.replaceAll("\\s", ""));

        COSESign1 cwt = validator.parseCwt(cwtBytes);
        validator.validateProtectedHeader(cwt);
        MapElement claims = validator.decodePayload(cwt);
        StringElement subject = assertInstanceOf(StringElement.class,
                claims.getValue().get(new MapKey(CWT_SUBJECT_CLAIM)));
        NumberElement issuedAt = assertInstanceOf(NumberElement.class,
                claims.getValue().get(new MapKey(CWT_ISSUED_AT_CLAIM)));
        MapElement statusList = assertInstanceOf(MapElement.class,
                claims.getValue().get(new MapKey(CWT_STATUS_LIST_CLAIM)));
        NumberElement bits = assertInstanceOf(NumberElement.class,
                statusList.getValue().get(new MapKey(STATUS_LIST_BITS)));
        ByteStringElement lst = assertInstanceOf(ByteStringElement.class,
                statusList.getValue().get(new MapKey(STATUS_LIST_DATA)));

        assertEquals("https://example.com/statuslists/1", subject.getValue());
        assertEquals(1686920170L, issuedAt.getValue().longValue());
        assertEquals(1, bits.getValue().intValue());
        assertEquals(1, StatusListDecoder.resolveStatus(0, bits.getValue().intValue(), lst.getValue()));

    }

    @Test
    @DisplayName("and CWT is valid then status should be resolved")
    void validateAndResolveStatusReturnsStatusWhenCwtIsValid() throws Exception {
        StatusListCwtValidator validator = new StatusListCwtValidator(Duration.ofSeconds(60));
        URI expectedUri = URI.create("https://status.example/list/1");
        Instant now = Instant.now();
        byte[] cwt = createSignedStatusListCwtWithRsa(expectedUri, now, 1, new byte[]{0});

        int status = validator.validateAndResolveStatus(expectedUri, 1, cwt, now);

        assertEquals(0, status);
    }

    @Test
    @DisplayName("and status value is non-zero then non-zero status should be resolved")
    void validateAndResolveStatusReturnsNonZeroStatus() throws Exception {
        StatusListCwtValidator validator = new StatusListCwtValidator(Duration.ofSeconds(60));
        URI expectedUri = URI.create("https://status.example/list/1");
        Instant now = Instant.now();
        byte[] cwt = createSignedStatusListCwtWithRsa(expectedUri, now, 1, new byte[]{2});

        int status = validator.validateAndResolveStatus(expectedUri, 1, cwt, now);

        assertEquals(1, status);
    }

    @Test
    @DisplayName("and time is just before expiration plus clock skew then status should be resolved")
    void validateAndResolveStatusAcceptsCwtJustBeforeExpirationBoundary() throws Exception {
        StatusListCwtValidator validator = new StatusListCwtValidator(CLOCK_SKEW);
        Instant expirationTime = FIXED_NOW.plusSeconds(300);
        byte[] cwt = createSignedStatusListCwtWithRsa(
                STATUS_LIST_URI, FIXED_NOW.minusSeconds(30), expirationTime, 1, new byte[]{0});

        int status = validator.validateAndResolveStatus(
                STATUS_LIST_URI, 0, cwt, expirationTime.plus(CLOCK_SKEW).minus(1, NANOS));

        assertEquals(0, status);
    }

    @Test
    @DisplayName("and time is at expiration plus clock skew then verification exception should be thrown")
    void validateAndResolveStatusRejectsCwtAtExpirationBoundary() throws Exception {
        StatusListCwtValidator validator = new StatusListCwtValidator(CLOCK_SKEW);
        Instant expirationTime = FIXED_NOW.plusSeconds(300);
        byte[] cwt = createSignedStatusListCwtWithRsa(
                STATUS_LIST_URI, FIXED_NOW.minusSeconds(30), expirationTime, 1, new byte[]{0});

        VerificationException exception = assertThrows(VerificationException.class,
                () -> validator.validateAndResolveStatus(
                        STATUS_LIST_URI, 0, cwt, expirationTime.plus(CLOCK_SKEW)));

        assertEquals("Status list CWT is expired", exception.getErrorDescription());
    }

    @Test
    @DisplayName("and time is after expiration plus clock skew then verification exception should be thrown")
    void validateAndResolveStatusRejectsCwtAfterExpirationBoundary() throws Exception {
        StatusListCwtValidator validator = new StatusListCwtValidator(CLOCK_SKEW);
        Instant expirationTime = FIXED_NOW.plusSeconds(300);
        byte[] cwt = createSignedStatusListCwtWithRsa(
                STATUS_LIST_URI, FIXED_NOW.minusSeconds(30), expirationTime, 1, new byte[]{0});

        VerificationException exception = assertThrows(VerificationException.class,
                () -> validator.validateAndResolveStatus(
                        STATUS_LIST_URI, 0, cwt, expirationTime.plus(CLOCK_SKEW).plusNanos(1)));

        assertEquals("Status list CWT is expired", exception.getErrorDescription());
    }

    @Test
    @DisplayName("and issued-at equals now plus clock skew then status should be resolved")
    void validateAndResolveStatusAcceptsCwtIssuedAtClockSkewBoundary() throws Exception {
        StatusListCwtValidator validator = new StatusListCwtValidator(CLOCK_SKEW);
        byte[] cwt = createSignedStatusListCwtWithRsa(
                STATUS_LIST_URI, FIXED_NOW.plus(CLOCK_SKEW), FIXED_NOW.plusSeconds(300), 1, new byte[]{0});

        int status = validator.validateAndResolveStatus(STATUS_LIST_URI, 0, cwt, FIXED_NOW);

        assertEquals(0, status);
    }

    @Test
    @DisplayName("and issued-at is beyond now plus clock skew then verification exception should be thrown")
    void validateAndResolveStatusRejectsCwtIssuedAfterClockSkewBoundary() throws Exception {
        StatusListCwtValidator validator = new StatusListCwtValidator(CLOCK_SKEW);
        byte[] cwt = createSignedStatusListCwtWithRsa(
                STATUS_LIST_URI, FIXED_NOW.plus(CLOCK_SKEW).plusSeconds(1), FIXED_NOW.plusSeconds(300),
                1, new byte[]{0});

        VerificationException exception = assertThrows(VerificationException.class,
                () -> validator.validateAndResolveStatus(STATUS_LIST_URI, 0, cwt, FIXED_NOW));

        assertEquals("Status list CWT iat is in the future", exception.getErrorDescription());
    }

    @Test
    @DisplayName("and status list subject does not match expected URI then verification exception should be thrown")
    void validateAndResolveStatusThrowsWhenSubjectDoesNotMatchExpectedUri() throws Exception {
        StatusListCwtValidator validator = new StatusListCwtValidator(Duration.ofSeconds(60));
        Instant now = Instant.now();
        byte[] cwt = createSignedStatusListCwtWithRsa(
                URI.create("https://status.example/list/other"), now, 1, new byte[]{0});

        VerificationException exception = assertThrows(VerificationException.class,
                () -> validator.validateAndResolveStatus(
                        URI.create("https://status.example/list/expected"), 0, cwt, now));

        assertEquals("Status list CWT sub does not match expected status list uri", exception.getErrorDescription());
    }

    @Test
    @DisplayName("and CWT signature is invalid then verification exception should be thrown")
    void validateAndResolveStatusThrowsWhenSignatureIsInvalid() throws Exception {
        StatusListCwtValidator validator = new StatusListCwtValidator(Duration.ofSeconds(60));
        URI expectedUri = URI.create("https://status.example/list/1");
        byte[] cwt = createSignedStatusListCwtWithRsa(expectedUri, Instant.now(), 1, new byte[]{0});
        cwt[cwt.length - 1] ^= 1;

        VerificationException exception = assertThrows(VerificationException.class,
                () -> validator.validateAndResolveStatus(expectedUri, 0, cwt, Instant.now()));

        assertEquals("Status list CWT signature validation failed", exception.getErrorDescription());
    }
}
