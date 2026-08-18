package no.idporten.eudiw.verifier.statuslist;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.verifier.IOConnectionException;
import no.idporten.eudiw.verifier.StatusCommunicationException;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import static no.idporten.eudiw.verifier.statuslist.StatusListTestUtil.createSignedStatusListJwtWithRsa;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("When checking token status list service behavior")
class TokenStatuslistServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    private TokenStatuslistService service;

    @BeforeEach
    void setUp() {
        service = new TokenStatuslistService(restClient, config());
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
    @DisplayName("and x5c certificate chain is missing then verification exception should be thrown")
    void checkStatusThrowsVerificationExceptionWhenX5cIsMissing() {
        URI uri = URI.create("https://status.example/list/1");
        String jwtWithoutX5c = createCompactJwtWithoutX5c();

        VerificationException exception = assertThrows(VerificationException.class,
                () -> service.checkStatus(uri, 0, jwtWithoutX5c, Instant.now()));

        assertEquals("Status list JWT must include x5c certificate chain", exception.getErrorDescription());
    }

    @Test
    @DisplayName("and endpoint returns a signed JWT then parsed JWT should be returned")
    void requestStatusListReturnsSignedJwtWhenEndpointReturnsJwt() throws Exception {
        URI url = URI.create("https://status.example/list/1");
        String jwt = createSimpleSignedJwt();
        when(restClient.get().uri(url).retrieve().body(String.class)).thenReturn(jwt);

        JWT parsed = service.requestStatusList(url);

        assertEquals(SignedJWT.class, parsed.getClass());
    }

    @Test
    @DisplayName("and URL is null then verification exception should be thrown")
    void requestStatusListThrowsWhenUrlIsNull() {
        assertThrows(VerificationException.class, () -> service.requestStatusList(null));
    }

    @Test
    @DisplayName("and endpoint times out then IO connection exception should be thrown")
    void requestStatusListThrowsIoConnectionExceptionOnResourceAccessException() {
        URI url = URI.create("https://status.example/list/1");
        when(restClient.get().uri(url).retrieve().body(String.class)).thenThrow(new ResourceAccessException("timeout"));

        assertThrows(IOConnectionException.class, () -> service.requestStatusList(url));
    }

    @Test
    @DisplayName("and endpoint fails unexpectedly then status communication exception should be thrown")
    void requestStatusListThrowsStatusCommunicationExceptionOnOtherErrors() {
        URI url = URI.create("https://status.example/list/1");
        when(restClient.get().uri(url).retrieve().body(String.class)).thenThrow(new IllegalStateException("boom"));

        assertThrows(StatusCommunicationException.class, () -> service.requestStatusList(url));
    }

    @Test
    @DisplayName("and response body is null then verification exception should be thrown")
    void requestStatusListThrowsVerificationExceptionWhenBodyIsNull() {
        URI url = URI.create("https://status.example/list/1");
        when(restClient.get().uri(url).retrieve().body(String.class)).thenReturn(null);

        assertThrows(VerificationException.class, () -> service.requestStatusList(url));
    }

    @Test
    @DisplayName("and response body is not a JWT then verification exception should be thrown")
    void requestStatusListThrowsVerificationExceptionWhenJwtCannotBeParsed() {
        URI url = URI.create("https://status.example/list/1");
        when(restClient.get().uri(url).retrieve().body(String.class)).thenReturn("not-a-jwt");

        assertThrows(VerificationException.class, () -> service.requestStatusList(url));
    }

    private static TokenStatuslistConfig config() {
        return new TokenStatuslistConfig(Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(60));
    }

    private static String createSimpleSignedJwt() throws Exception {
        URI uri = URI.create("https://status.example/list/1");
        Instant now = Instant.now();
        return createSignedStatusListJwtWithRsa(uri, now, 0, 0);
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
