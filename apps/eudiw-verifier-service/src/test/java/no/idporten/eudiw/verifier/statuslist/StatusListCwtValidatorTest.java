package no.idporten.eudiw.verifier.statuslist;

import no.idporten.eudiw.verifier.VerificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import static no.idporten.eudiw.verifier.statuslist.StatusListTestUtil.createSignedStatusListCwtWithRsa;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("When validating and resolving status from status list CWT")
class StatusListCwtValidatorTest {

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
