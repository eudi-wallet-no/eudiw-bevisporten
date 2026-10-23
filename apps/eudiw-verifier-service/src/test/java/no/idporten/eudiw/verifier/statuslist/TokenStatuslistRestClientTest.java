package no.idporten.eudiw.verifier.statuslist;

import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import static no.idporten.eudiw.verifier.statuslist.StatusListTestUtil.createSignedStatusListCwtWithRsa;
import static no.idporten.eudiw.verifier.statuslist.StatusListTestUtil.createSignedStatusListJwtWithRsa;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("When fetching a token status list")
class TokenStatuslistRestClientTest {

    private MockRestServiceServer mockServer;
    private TokenStatuslistService service;

    @BeforeEach
    void setUp() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(3));

        RestClient.Builder builder = RestClient.builder().requestFactory(factory);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        service = new TokenStatuslistService(
                builder.build(),
                new TokenStatuslistConfig(Duration.ofSeconds(3), Duration.ofSeconds(3), Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("and CWT is preferred but not acceptable then JWT should be requested as fallback")
    void requestsCwtFirstAndFallsBackToJwt() throws Exception {
        URI uri = URI.create("https://status.example/list/1");
        String jwt = createSignedStatusListJwtWithRsa(uri, Instant.now(), 0, 0);

        mockServer.expect(requestTo(uri))
                .andExpect(header(HttpHeaders.ACCEPT, StatusListTokenFormat.CWT.mediaType().toString()))
                .andRespond(withStatus(HttpStatus.NOT_ACCEPTABLE));
        mockServer.expect(requestTo(uri))
                .andExpect(header(HttpHeaders.ACCEPT, StatusListTokenFormat.JWT.mediaType().toString()))
                .andRespond(withSuccess(jwt, StatusListTokenFormat.JWT.mediaType()));

        ValidationStatus status = service.lookupStatusFromStatuslist(uri, 0, StatusListTokenFormat.CWT);

        assertEquals(ValidationStatus.VALID, status);
        mockServer.verify();
    }

    @Test
    @DisplayName("and JWT is preferred and available then it should be requested without fallback")
    void requestsJwtFirstAndDoesNotFallbackWhenAvailable() throws Exception {
        URI uri = URI.create("https://status.example/list/1");
        String jwt = createSignedStatusListJwtWithRsa(uri, Instant.now(), 0, 0);

        mockServer.expect(requestTo(uri))
                .andExpect(header(HttpHeaders.ACCEPT, StatusListTokenFormat.JWT.mediaType().toString()))
                .andRespond(withSuccess(jwt, StatusListTokenFormat.JWT.mediaType()));

        ValidationStatus status = service.lookupStatusFromStatuslist(uri, 0, StatusListTokenFormat.JWT);

        assertEquals(ValidationStatus.VALID, status);
        mockServer.verify();
    }

    @Test
    @DisplayName("and endpoint ignores CWT preference and returns JWT then returned JWT should be validated")
    void validatesJwtWhenEndpointDoesNotNegotiatePreferredCwt() throws Exception {
        URI uri = URI.create("https://status.example/list/1");
        String jwt = createSignedStatusListJwtWithRsa(uri, Instant.now(), 0, 0);

        mockServer.expect(requestTo(uri))
                .andExpect(header(HttpHeaders.ACCEPT, StatusListTokenFormat.CWT.mediaType().toString()))
                .andRespond(withSuccess(jwt, StatusListTokenFormat.JWT.mediaType()));

        ValidationStatus status = service.lookupStatusFromStatuslist(uri, 0, StatusListTokenFormat.CWT);

        assertEquals(ValidationStatus.VALID, status);
        mockServer.verify();
    }

    @Test
    @DisplayName("and endpoint returns CWT then CWT response should be validated")
    void validatesCwtResponse() throws Exception {
        URI uri = URI.create("https://status.example/list/1");
        byte[] cwt = createSignedStatusListCwtWithRsa(uri, Instant.now(), 1, new byte[]{0});

        mockServer.expect(requestTo(uri))
                .andExpect(header(HttpHeaders.ACCEPT, StatusListTokenFormat.CWT.mediaType().toString()))
                .andRespond(withSuccess(cwt, StatusListTokenFormat.CWT.mediaType()));

        ValidationStatus status = service.lookupStatusFromStatuslist(uri, 0, StatusListTokenFormat.CWT);

        assertEquals(ValidationStatus.VALID, status);
        mockServer.verify();
    }

    @Test
    @DisplayName("and JWT is preferred but not acceptable then CWT should be requested as fallback")
    void requestsJwtFirstAndFallsBackToCwt() throws Exception {
        URI uri = URI.create("https://status.example/list/1");
        byte[] cwt = createSignedStatusListCwtWithRsa(uri, Instant.now(), 1, new byte[]{0});

        mockServer.expect(requestTo(uri))
                .andExpect(header(HttpHeaders.ACCEPT, StatusListTokenFormat.JWT.mediaType().toString()))
                .andRespond(withStatus(HttpStatus.NOT_ACCEPTABLE));
        mockServer.expect(requestTo(uri))
                .andExpect(header(HttpHeaders.ACCEPT, StatusListTokenFormat.CWT.mediaType().toString()))
                .andRespond(withSuccess(cwt, StatusListTokenFormat.CWT.mediaType()));

        ValidationStatus status = service.lookupStatusFromStatuslist(uri, 0, StatusListTokenFormat.JWT);

        assertEquals(ValidationStatus.VALID, status);
        mockServer.verify();
    }

    @Test
    @DisplayName("and neither format is acceptable then status should be inconclusive")
    void returnsInconclusiveWhenNeitherFormatIsAccepted() {
        URI uri = URI.create("https://status.example/list/1");

        mockServer.expect(requestTo(uri))
                .andExpect(header(HttpHeaders.ACCEPT, StatusListTokenFormat.JWT.mediaType().toString()))
                .andRespond(withStatus(HttpStatus.NOT_ACCEPTABLE));
        mockServer.expect(requestTo(uri))
                .andExpect(header(HttpHeaders.ACCEPT, StatusListTokenFormat.CWT.mediaType().toString()))
                .andRespond(withStatus(HttpStatus.NOT_ACCEPTABLE));

        ValidationStatus status = service.lookupStatusFromStatuslist(uri, 0, StatusListTokenFormat.JWT);

        assertEquals(ValidationStatus.INCONCLUSIVE, status);
        mockServer.verify();
    }

    @Test
    @DisplayName("and preferred format response is invalid then fallback should not be requested")
    void doesNotFallbackWhenPreferredResponseIsInvalid() throws Exception {
        URI uri = URI.create("https://status.example/list/1");

        mockServer.expect(requestTo(uri))
                .andExpect(header(HttpHeaders.ACCEPT, StatusListTokenFormat.CWT.mediaType().toString()))
                .andRespond(withSuccess(new byte[]{1, 2, 3}, StatusListTokenFormat.CWT.mediaType()));

        org.junit.jupiter.api.Assertions.assertThrows(
                no.idporten.eudiw.verifier.VerificationException.class,
                () -> service.lookupStatusFromStatuslist(uri, 0, StatusListTokenFormat.CWT));

        mockServer.verify();
    }
}
