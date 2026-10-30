package no.idporten.eudiw.verifier.statuslist;

import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import static no.idporten.eudiw.verifier.statuslist.StatusListTestUtil.createSignedStatusListCwtWithRsa;
import static no.idporten.eudiw.verifier.statuslist.StatusListTestUtil.createSignedStatusListJwtWithRsa;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@DisplayName("When checking token status list service behavior")
class TokenStatuslistServiceTest {

    private TokenStatuslistService service;

    @BeforeEach
    void setUp() {
        service = new TokenStatuslistService(RestClient.create(), config());
    }

    @Test
    @DisplayName("and status value is zero then result should be valid")
    void checkStatusReturnsValidWhenStatusAtIndexIsZero() throws Exception {
        URI uri = URI.create("https://status.example/list/1");
        Instant now = Instant.now();
        String jwt = createSignedStatusListJwtWithRsa(uri, now, 0, 0);

        ValidationStatus status = service.checkStatus(uri, 0, jwt, now);

        assertEquals(ValidationStatus.VALID, status);
    }

    @Test
    @DisplayName("and status value is non-zero then result should be invalid")
    void checkStatusReturnsInvalidWhenStatusAtIndexIsNonZero() throws Exception {
        URI uri = URI.create("https://status.example/list/1");
        Instant now = Instant.now();
        String jwt = createSignedStatusListJwtWithRsa(uri, now, 0, 1);

        ValidationStatus status = service.checkStatus(uri, 0, jwt, now);

        assertEquals(ValidationStatus.INVALID, status);
    }

    @Test
    @DisplayName("and CWT status value is zero then result should be valid")
    void checkStatusReturnsValidWhenCwtStatusAtIndexIsZero() throws Exception {
        URI uri = URI.create("https://status.example/list/1");
        Instant now = Instant.now();
        byte[] cwt = createSignedStatusListCwtWithRsa(uri, now, 1, new byte[]{0});

        ValidationStatus status = service.checkStatus(uri, 0, cwt, now);

        assertEquals(ValidationStatus.VALID, status);
    }

    @Test
    @DisplayName("and x5c certificate chain is missing then verification exception should be thrown")
    void checkStatusThrowsVerificationExceptionWhenX5cIsMissing() {
        URI uri = URI.create("https://status.example/list/1");
        String jwtWithoutX5c = createCompactJwtWithoutX5c();

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.checkStatus(uri, 0, jwtWithoutX5c, Instant.now()));

        assertEquals("Status list JWT must include x5c certificate chain", exception.getErrorDescription());
    }

    @Test
    @DisplayName("and key is referenced by kid instead of x5c then verification exception should state that key references are not supported")
    void checkStatusThrowsVerificationExceptionWhenKeyIsReferencedByKid() {
        URI uri = URI.create("https://status.example/list/1");
        String header = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"statuslist+jwt\",\"kid\":\"key-1\"}".getBytes());
        String jwt = header + "." + createCompactJwtWithoutX5c().split("\\.")[1] + ".c2ln";

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.checkStatus(uri, 0, jwt, Instant.now()));

        assertEquals("Status list JWT key resolution by kid is not supported yet, x5c is required", exception.getErrorDescription());
    }

    private static TokenStatuslistConfig config() {
        return new TokenStatuslistConfig(Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(60));
    }

    private static String createCompactJwtWithoutX5c() {
        String header = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"statuslist+jwt\"}".getBytes());
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"https://status.example/list/1\",\"iat\":1,\"status_list\":{\"bits\":1,\"lst\":\"eAEBAP__AAABAAE\"}}".getBytes());
        String signature = "c2ln";
        return header + "." + payload + "." + signature;
    }
}
