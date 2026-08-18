package no.idporten.eudiw.verifier.statuslist;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.MACVerifier;
import no.idporten.eudiw.verifier.VerificationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static no.idporten.eudiw.verifier.statuslist.StatusListTestUtil.HMAC_SECRET;
import static no.idporten.eudiw.verifier.statuslist.StatusListTestUtil.createSignedStatusListJwtWithHmac;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("When validating and resolving status from status list JWT")
class StatusListJwtValidatorTest {

    @Test
    @DisplayName("and JWT is valid then status should be resolved")
    void validateAndResolveStatusReturnsStatusWhenJwtIsValid() throws Exception {
        StatusListJwtValidator validator = new StatusListJwtValidator(Set.of(JWSAlgorithm.HS256), Duration.ofSeconds(60));
        URI expectedUri = URI.create("https://status.example/list/1");
        Instant now = Instant.now();
        String jwt = createSignedStatusListJwtWithHmac(expectedUri, now, 2, new byte[]{(byte) 0b1110_0100});

        int status = validator.validateAndResolveStatus(expectedUri, 2, jwt, now, new MACVerifier(HMAC_SECRET));

        assertEquals(2, status);
    }

    @Test
    @DisplayName("and subject does not match expected URI then verification exception should be thrown")
    void validateAndResolveStatusThrowsWhenSubjectDoesNotMatchExpectedUri() throws Exception {
        StatusListJwtValidator validator = new StatusListJwtValidator(Set.of(JWSAlgorithm.HS256), Duration.ofSeconds(60));
        Instant now = Instant.now();
        URI expectedUri = URI.create("https://status.example/list/expected");
        String jwt = createSignedStatusListJwtWithHmac(URI.create("https://status.example/list/other"), now, 1, new byte[]{0});

        VerificationException exception = assertThrows(VerificationException.class,
                () -> validator.validateAndResolveStatus(expectedUri, 0, jwt, now, new MACVerifier(HMAC_SECRET)));

        assertEquals("Status list JWT sub does not match expected status list uri", exception.getErrorDescription());
    }

    @Test
    @DisplayName("and JWT is expired then verification exception should be thrown")
    void validateAndResolveStatusThrowsWhenJwtIsExpired() throws Exception {
        StatusListJwtValidator validator = new StatusListJwtValidator(Set.of(JWSAlgorithm.HS256), Duration.ofSeconds(5));
        URI expectedUri = URI.create("https://status.example/list/1");
        Instant now = Instant.now();
        String jwt = createSignedStatusListJwtWithHmac(expectedUri, now.minusSeconds(500), 1, new byte[]{0});

        VerificationException exception = assertThrows(VerificationException.class,
                () -> validator.validateAndResolveStatus(expectedUri, 0, jwt, now, new MACVerifier(HMAC_SECRET)));

        assertEquals("Status list JWT is expired", exception.getErrorDescription());
    }

    @Test
    @DisplayName("and index is out of bounds then verification exception should be thrown")
    void validateAndResolveStatusThrowsWhenIdxIsOutOfBounds() throws Exception {
        StatusListJwtValidator validator = new StatusListJwtValidator(Set.of(JWSAlgorithm.HS256), Duration.ofSeconds(60));
        URI expectedUri = URI.create("https://status.example/list/1");
        Instant now = Instant.now();
        String jwt = createSignedStatusListJwtWithHmac(expectedUri, now, 1, new byte[]{0});

        VerificationException exception = assertThrows(VerificationException.class,
                () -> validator.validateAndResolveStatus(expectedUri, 8, jwt, now, new MACVerifier(HMAC_SECRET)));

        assertEquals("status_list.idx is out of bounds", exception.getErrorDescription());
    }
}
