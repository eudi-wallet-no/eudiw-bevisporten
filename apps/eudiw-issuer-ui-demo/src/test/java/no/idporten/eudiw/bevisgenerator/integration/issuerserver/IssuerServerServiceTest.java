package no.idporten.eudiw.bevisgenerator.integration.issuerserver;

import no.idporten.eudiw.bevisgenerator.byob.CredentialIssuerService;
import no.idporten.eudiw.bevisgenerator.exception.IssuerServerException;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.lib.maskinporten.client.MaskinportenClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

class IssuerServerServiceTest {

    private static final String UNAVAILABLE_URL = "http://issuer-server:9240/.well-known/openid-credential-issuer/pid";
    private static final String AVAILABLE_URL = "http://issuer-server:9241/.well-known/openid-credential-issuer/proof-of-age";

    private MockRestServiceServer mockServer;
    private RestClient restClient;
    private IssuerServerService issuerServerService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        issuerServerService = createIssuerServerService(List.of(UNAVAILABLE_URL, AVAILABLE_URL));
    }

    private IssuerServerService createIssuerServerService(List<String> wellKnownUrls) {
        IssuerServerProperties properties = new IssuerServerProperties(
                "http://issuer-server",
                "/credential",
                List.of(),
                List.of(),
                wellKnownUrls
        );
        return new IssuerServerService(
                restClient,
                properties,
                mock(MaskinportenClient.class),
                mock(CredentialIssuerService.class)
        );
    }

    @Test
    void throwsIssuerExceptionWhenEndpointIsUnavailable() {
        mockServer.expect(requestTo(UNAVAILABLE_URL))
                .andRespond(withException(new ConnectException("Connection refused")));

        assertThatThrownBy(() -> issuerServerService.getAllCredentialIssuerMetadata())
                .isInstanceOf(IssuerServerException.class)
                .hasMessage("Unable to fetch .well-known endpoint");
        mockServer.verify();
    }


    @Test
    void throwsIssuerExceptionWhenEndpointReturnsServerError() {
        issuerServerService = createIssuerServerService(List.of(AVAILABLE_URL));
        mockServer.expect(requestTo(AVAILABLE_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> issuerServerService.getAllCredentialIssuerMetadata())
                .isInstanceOf(IssuerServerException.class)
                .hasMessage("Server error fetching .well-known endpoint");
        mockServer.verify();
    }
}
