package no.idporten.eudiw.bevisgenerator.integration.issuerserver;

import no.idporten.eudiw.bevisgenerator.byob.CredentialIssuerService;
import no.idporten.eudiw.bevisgenerator.config.FeatureSwitches;
import no.idporten.eudiw.bevisgenerator.exception.IssuerServerException;
import no.idporten.eudiw.bevisgenerator.exception.IssuerUiException;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.CredentialConfiguration;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.config.IssuerServerProperties;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain.IssuanceStatus;
import no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain.IssuanceStatusResponse;
import no.idporten.eudiw.bevisgenerator.web.models.StartIssuanceForm;
import no.idporten.lib.maskinporten.client.AccessTokenRequestOverrides;
import no.idporten.lib.maskinporten.client.MaskinportenClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;

class IssuerServerServiceTest {

    private static final String STATUS_ENDPOINT =
            "http://issuer/tenant/api/v1/credential/issuance-transaction/tx-id";
    private static final String UNAVAILABLE_URL =
            "http://issuer-server:9240/.well-known/openid-credential-issuer/pid";
    private static final String AVAILABLE_URL =
            "http://issuer-server:9241/.well-known/openid-credential-issuer/proof-of-age";

    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersSpec requestHeadersSpec;
    private RestClient.ResponseSpec responseSpec;
    private IssuerServerService issuerServerService;
    private CredentialConfiguration credentialConfiguration;
    private FeatureSwitches featureSwitches;

    @BeforeEach
    void setUp() {
        RestClient restClient = mock(RestClient.class);
        IssuerServerProperties properties = new IssuerServerProperties(
                "http://issuer",
                "/api/v1/credential/issuance-transaction",
                null,
                null,
                null
        );
        MaskinportenClient maskinportenClient = mock(MaskinportenClient.class, RETURNS_DEEP_STUBS);
        when(maskinportenClient.getAccessToken(any(AccessTokenRequestOverrides.class)).getValue())
                .thenReturn("access-token");

        requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class, RETURNS_SELF);
        responseSpec = mock(RestClient.ResponseSpec.class);
        featureSwitches = mock(FeatureSwitches.class);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(STATUS_ENDPOINT)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        issuerServerService = new IssuerServerService(
                restClient,
                properties,
                maskinportenClient,
                mock(CredentialIssuerService.class),
                featureSwitches
        );
        credentialConfiguration = new CredentialConfiguration(
                "http://issuer/tenant",
                "pid",
                "eudiw:pid",
                null,
                "PID",
                "{}"
        );
    }

    @Test
    void retrieveIssuanceStatusReturnsIssuerResponse() {
        IssuanceStatusResponse expected = new IssuanceStatusResponse(
                "tx-id",
                IssuanceStatus.CREDENTIAL_ACCEPTED
        );
        when(responseSpec.body(IssuanceStatusResponse.class)).thenReturn(expected);

        IssuanceStatusResponse actual = issuerServerService.retrieveIssuanceStatus(
                credentialConfiguration,
                "tx-id"
        );

        assertEquals(expected, actual);
        verify(requestHeadersUriSpec).uri(STATUS_ENDPOINT);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<HttpHeaders>> headersCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(requestHeadersSpec).headers(headersCaptor.capture());
        HttpHeaders headers = new HttpHeaders();
        headersCaptor.getValue().accept(headers);
        assertEquals("Bearer access-token", headers.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void retrieveIssuanceStatusRejectsEmptyResponse() {
        when(responseSpec.body(IssuanceStatusResponse.class)).thenReturn(null);

        IssuerUiException exception = assertThrows(
                IssuerUiException.class,
                () -> issuerServerService.retrieveIssuanceStatus(credentialConfiguration, "tx-id")
        );

        assertEquals(
                "Issuer-server returned null issuance status for issuance_transaction_id=tx-id",
                exception.getMessage()
        );
    }

    @Test
    void startIssuanceSendsBearerAccessToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        IssuerServerProperties properties = new IssuerServerProperties(
                "http://issuer",
                "/api/v1/credential/issuance-transaction",
                null,
                null,
                null
        );
        MaskinportenClient maskinportenClient = mock(MaskinportenClient.class, RETURNS_DEEP_STUBS);
        when(maskinportenClient.getAccessToken(any(AccessTokenRequestOverrides.class)).getValue())
                .thenReturn("access-token");
        IssuerServerService service = new IssuerServerService(
                builder.build(),
                properties,
                maskinportenClient,
                mock(CredentialIssuerService.class),
                featureSwitches
        );

        mockServer.expect(requestTo("http://issuer/tenant/api/v1/credential/issuance-transaction"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andRespond(withSuccess("""
                        {
                            "credential_offer": {
                                "credential_issuer": "http://issuer/tenant",
                                "credential_configuration_ids": ["pid"]
                            },
                            "issuance_transaction_id": "tx-id"
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = service.startIssuance(
                credentialConfiguration,
                new StartIssuanceForm("{}", "subject-id")
        );

        assertEquals("tx-id", response.issuanceTransactionId());
        mockServer.verify();
    }

    @Test
    void revokeCredentialUsesV1WithoutFallbackWhenRichResultIsDisabled() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        IssuerServerService service = createRevocationService(builder.build(), false);
        mockServer.expect(requestTo("http://issuer/tenant/api/v1/credential/revoke"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withNoContent());

        OptionalInt result = service.revokeCredential(credentialConfiguration, "tx-id");

        assertEquals(OptionalInt.empty(), result);
        mockServer.verify();
    }

    @Test
    void revokeCredentialUsesV2AndReturnsCountWhenRichResultIsEnabled() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        IssuerServerService service = createRevocationService(builder.build(), true);
        mockServer.expect(requestTo("http://issuer/tenant/api/v2/credential/revoke"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{\"revokedCount\":1}", MediaType.APPLICATION_JSON));

        OptionalInt result = service.revokeCredential(credentialConfiguration, "tx-id");

        assertEquals(OptionalInt.of(1), result);
        mockServer.verify();
    }

    @Test
    void revokeCredentialBySubjectUsesConfiguredApiVersion() {
        RestClient.Builder v1Builder = RestClient.builder();
        MockRestServiceServer v1Server = MockRestServiceServer.bindTo(v1Builder).build();
        IssuerServerService v1Service = createRevocationService(v1Builder.build(), false);
        v1Server.expect(requestTo("http://issuer/tenant/api/v1/credential/revoke/by-subject"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withNoContent());

        assertEquals(
                OptionalInt.empty(),
                v1Service.revokeCredentialBySubject(credentialConfiguration, "subject-id")
        );
        v1Server.verify();

        RestClient.Builder v2Builder = RestClient.builder();
        MockRestServiceServer v2Server = MockRestServiceServer.bindTo(v2Builder).build();
        IssuerServerService v2Service = createRevocationService(v2Builder.build(), true);
        v2Server.expect(requestTo("http://issuer/tenant/api/v2/credential/revoke/by-subject"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("{\"revokedCount\":2}", MediaType.APPLICATION_JSON));

        assertEquals(
                OptionalInt.of(2),
                v2Service.revokeCredentialBySubject(credentialConfiguration, "subject-id")
        );
        v2Server.verify();
    }

    @Test
    void throwsIssuerExceptionWhenEndpointIsUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        IssuerServerService metadataService = createMetadataService(
                builder.build(),
                List.of("http://issuer-server:9240/.well-known/openid-credential-issuer/pid")
        );
        mockServer.expect(requestTo(UNAVAILABLE_URL))
                .andRespond(withException(new ConnectException("Connection refused")));

        assertThatThrownBy(metadataService::getAllCredentialIssuerMetadata)
                .isInstanceOf(IssuerServerException.class)
                .hasMessage("Unable to fetch .well-known endpoint");
        mockServer.verify();
    }

    @Test
    void throwsIssuerExceptionWhenEndpointReturnsServerError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        IssuerServerService metadataService = createMetadataService(
                builder.build(),
                List.of("http://issuer-server:9241/.well-known/openid-credential-issuer/proof-of-age")
        );
        mockServer.expect(requestTo(AVAILABLE_URL))
                .andRespond(withServerError());

        assertThatThrownBy(metadataService::getAllCredentialIssuerMetadata)
                .isInstanceOf(IssuerServerException.class)
                .hasMessage("Server error fetching .well-known endpoint");
        mockServer.verify();
    }

    private IssuerServerService createMetadataService(RestClient restClient, List<String> wellKnownUrls) {
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
                mock(CredentialIssuerService.class),
                mock(FeatureSwitches.class)
        );
    }

    private IssuerServerService createRevocationService(RestClient restClient, boolean richResultEnabled) {
        IssuerServerProperties properties = new IssuerServerProperties(
                "http://issuer",
                "/api/v1/credential/issuance-transaction",
                null,
                null,
                null
        );
        MaskinportenClient maskinportenClient = mock(MaskinportenClient.class, RETURNS_DEEP_STUBS);
        when(maskinportenClient.getAccessToken(any(AccessTokenRequestOverrides.class)).getValue())
                .thenReturn("access-token");
        FeatureSwitches switches = mock(FeatureSwitches.class);
        when(switches.isRevocationV2RichResult()).thenReturn(richResultEnabled);

        return new IssuerServerService(
                restClient,
                properties,
                maskinportenClient,
                mock(CredentialIssuerService.class),
                switches
        );
    }
}
