package no.idporten.eudiw.verifier.statuslist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;

import static no.idporten.eudiw.verifier.statuslist.StatusListTestUtil.createSignedStatusListJwtWithRsa;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using token statuslist REST client")
class TokenStatuslistRestClientTest {

    @Autowired
    private TokenStatuslistConfig tokenStatuslistConfig;

    private RestClient restClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(tokenStatuslistConfig.connectTimeout());
        factory.setReadTimeout(tokenStatuslistConfig.readTimeout());

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, TokenStatuslistRestClient.STATUS_LIST_MEDIA_TYPE);

        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    @Test
    @DisplayName("and requesting a status list JWT then request should include statuslist+jwt accept header and response should be parsed successfully")
    void restClientShouldIncludeStatuslistJwtAcceptHeader() throws Exception {
        URI url = URI.create("https://status.example/list/1");
        String jwt = createSignedStatusListJwtWithRsa(url, java.time.Instant.now(), 0, 0);

        mockServer.expect(requestTo(url))
                .andExpect(header(HttpHeaders.ACCEPT, TokenStatuslistRestClient.STATUS_LIST_MEDIA_TYPE))
                .andRespond(withSuccess(jwt, MediaType.valueOf(TokenStatuslistRestClient.STATUS_LIST_MEDIA_TYPE)));

        String response = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertEquals(jwt, response);
        mockServer.verify();
    }


    @Test
    @DisplayName("and endpoint returns empty response then null should be returned")
    void restClientShouldHandleEmptyResponse() {
        URI url = URI.create("https://status.example/list/2");

        mockServer.expect(requestTo(url))
                .andRespond(withSuccess("", MediaType.valueOf(TokenStatuslistRestClient.STATUS_LIST_MEDIA_TYPE)));

        String response = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        assertNull(response);
    }

    @Test
    @DisplayName("and endpoint returns 204 no content then null should be returned")
    void restClientShouldHandleNoContent() {
        URI url = URI.create("https://status.example/list/3");

        mockServer.expect(requestTo(url))
                .andRespond(withNoContent());

        String response = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        assertNull(response);
    }
}
