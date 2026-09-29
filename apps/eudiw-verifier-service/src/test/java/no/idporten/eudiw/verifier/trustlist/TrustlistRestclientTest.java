package no.idporten.eudiw.verifier.trustlist;

import no.idporten.eudiw.verifier.testdata.TrustlistTestdata;
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

import static no.idporten.eudiw.verifier.testdata.TrustlistTestdata.getJsonTrustlist;
import static no.idporten.eudiw.verifier.testdata.TrustlistTestdata.getXmlTrustlist;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using trustlist REST client")
public class TrustlistRestclientTest {

    @Autowired
    private TrustlistsProperties trustlistProperties;


    private RestClient restClientJson;
    private MockRestServiceServer mockServerJson;
    private RestClient restClientXml;
    private MockRestServiceServer mockServerXml;

    public static final String TRUSTLIST_MEDIA_TYPE_XML = "application/vnd.etsi.tsl+xml";
    public static final String TRUSTLIST_MEDIA_TYPE_JSON = "application/jose+json";

    @BeforeEach
    void setUp() {
        // Both XML and JSON
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(trustlistProperties.connectTimeout());
        factory.setReadTimeout(trustlistProperties.readTimeout());

        // Only XML
        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, TRUSTLIST_MEDIA_TYPE_XML);

        mockServerXml = MockRestServiceServer.bindTo(builder).build();
        restClientXml = builder.build();

        // Only JSON
        RestClient.Builder builderJson = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, TRUSTLIST_MEDIA_TYPE_JSON);

        mockServerJson = MockRestServiceServer.bindTo(builderJson).build();
        restClientJson = builderJson.build();
    }

    @Test
    @DisplayName("and requesting a trustlist XML then request should include trustlist+xml accept header and response should be parsed successfully")
    void restClientShouldIncludeTrustlistXmlAcceptHeader() {

        URI xmlTrustlist = trustlistProperties.getAttestationTrustlists().getFirst().uri();
        mockServerXml.expect(requestTo(xmlTrustlist))
                .andExpect(header(HttpHeaders.ACCEPT, TRUSTLIST_MEDIA_TYPE_XML))
                .andRespond(withSuccess(getXmlTrustlist(),
                        new MediaType("application", "vnd.etsi.tsl+xml", java.nio.charset.StandardCharsets.UTF_8)));

        String response = restClientXml.get()
                .uri(xmlTrustlist)
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertEquals(getXmlTrustlist(), response);

        mockServerXml.verify();
    }

    @Test
    @DisplayName("and requesting a trustlist JSON then request should include trustlist+jws accept header and response should be parsed successfully")
    void restClientShouldIncludeTrustlistJsonAcceptHeader() {
        URI jsonTrustlist = trustlistProperties.getPidTrustlists().getFirst().uri();
        mockServerJson.expect(requestTo(jsonTrustlist))
                .andExpect(header(HttpHeaders.ACCEPT, TRUSTLIST_MEDIA_TYPE_JSON))
                .andRespond(withSuccess(getJsonTrustlist(), MediaType.valueOf(TRUSTLIST_MEDIA_TYPE_JSON)));

        String response = restClientJson.get()
                .uri(jsonTrustlist)
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertEquals(getJsonTrustlist(), response);

        mockServerJson.verify();
    }
}
