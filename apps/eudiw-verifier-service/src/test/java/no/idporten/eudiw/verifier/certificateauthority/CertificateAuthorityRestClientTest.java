package no.idporten.eudiw.verifier.certificateauthority;


import no.idporten.eudiw.verifier.testdata.Certificates;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("When using certificate authority REST client")
class CertificateAuthorityRestClientTest {

    @Autowired
    private CertificateAuthorityConfig certificateAuthorityConfig;

    private RestClient restClient;
    private MockRestServiceServer mockServer;
    private Certificates certificates = new Certificates();

    @BeforeEach
    void setUp() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(certificateAuthorityConfig.connectTimeout());
        factory.setReadTimeout(certificateAuthorityConfig.readTimeout());

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.ACCEPT, CertificateAuthorityRestClient.CA_MEDIA_TYPE);

        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    @Test
    @DisplayName("and requesting a certificate authorty certificate then request should include application/x-pem-file header and response should be parsed successfully")
    void restClientShouldIncludeStatuslistJwtAcceptHeader() {
        URI url = URI.create("https://ca.example/root");

        mockServer.expect(requestTo(url))
                .andExpect(header(HttpHeaders.ACCEPT, CertificateAuthorityRestClient.CA_MEDIA_TYPE))
                .andRespond(withSuccess("-----BEGIN CERTIFICATE-----"+certificates.rootCertificate()+"-----END CERTIFICATE-----", MediaType.valueOf(CertificateAuthorityRestClient.CA_MEDIA_TYPE)));

        String response = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        assertNotNull(response);
        assertEquals("-----BEGIN CERTIFICATE-----"+certificates.rootCertificate()+"-----END CERTIFICATE-----", response);
        mockServer.verify();
    }


    @Test
    @DisplayName("and endpoint returns empty response then null should be returned")
    void restClientShouldHandleEmptyResponse() {
        URI url = URI.create("https://ca.example/root");

        mockServer.expect(requestTo(url))
                .andExpect(header(HttpHeaders.ACCEPT, CertificateAuthorityRestClient.CA_MEDIA_TYPE))
                .andRespond(withSuccess("", MediaType.valueOf(CertificateAuthorityRestClient.CA_MEDIA_TYPE)));

        String response = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        assertNull(response);
    }

    @Test
    @DisplayName("and endpoint returns 204 no content then null should be returned")
    void restClientShouldHandleNoContent() {
        URI url = URI.create("https://ca.example/root");

        mockServer.expect(requestTo(url))
                .andExpect(header(HttpHeaders.ACCEPT, CertificateAuthorityRestClient.CA_MEDIA_TYPE))
                .andRespond(withNoContent());

        String response = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        assertNull(response);
    }
}
