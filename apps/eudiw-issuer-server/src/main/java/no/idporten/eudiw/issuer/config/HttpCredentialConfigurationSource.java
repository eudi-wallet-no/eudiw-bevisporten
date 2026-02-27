package no.idporten.eudiw.issuer.config;

import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfiguration;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfigurations;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceIOException;
import no.idporten.eudiw.issuer.credentials.formats.CredentialFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Reads multiple credential configurations from an external service over HTTP.
 */
public class HttpCredentialConfigurationSource implements CredentialConfigurationSource {

    private static final Logger log = LoggerFactory.getLogger(HttpCredentialConfigurationSource.class);

    public static final String API_KEY = "X-API-KEY";

    private final CredentialConfigurationSourceProperties properties;

    private final CopyOnWriteArrayList<ExtendedCredentialConfiguration> credentialConfigurations = new CopyOnWriteArrayList<>();

    public HttpCredentialConfigurationSource(CredentialConfigurationSourceProperties properties) {
        this.properties = properties;
        this.update();
    }

    @Override
    public CredentialConfigurationSourceProperties getProperties() {
        return properties;
    }

    @Override
    public List<ExtendedCredentialConfiguration> retrieve() {
        return credentialConfigurations;
    }

    @Override
    public void update() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(properties.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(properties.readTimeout());
        RestClient restClient = RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        try {
            DynamicCredentialConfigurations credentialConfigurations = restClient
                    .get()
                    .uri(properties.uri())
                    .header(API_KEY, properties.apiKey())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> handleErrorResponse(response))
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponse(response))
                    .body(DynamicCredentialConfigurations.class);

            if (credentialConfigurations == null || credentialConfigurations.getCredentialConfigurations() == null || credentialConfigurations.getCredentialConfigurations().isEmpty()) {
                log.warn("No credential configurations retrieved from {}", properties.uri());
                return;
            }
            log.info("Retrieved {} credential-configurations from {}", credentialConfigurations.getCredentialConfigurations().size(), properties.uri());
            List<ExtendedCredentialConfiguration> extendedCredentialConfigurations = credentialConfigurations.getCredentialConfigurations().stream().map(this::convert).toList();
            this.credentialConfigurations.clear();
            this.credentialConfigurations.addAll(extendedCredentialConfigurations);
        } catch (ResourceAccessException e) {
            throw new ClaimsSourceIOException("BYOB", "IO error when calling Byob-service", e);
        } catch (RestClientException e) {
            throw new ClaimsSourceException("BYOB", "server_error", "Failed to get information from Byob-service", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    void handleErrorResponse(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to get data from authoritative source. Status: %s, message: %s".formatted(response.getStatusCode(), body);
        throw new ClaimsSourceException("BYOB", "server_error", "Failed to get information from Byob-service", HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
    }

    protected ExtendedCredentialConfiguration convert(DynamicCredentialConfiguration dynamicCredentialConfiguration) {
        ExtendedCredentialConfiguration credentialConfiguration = new ExtendedCredentialConfiguration();
        // from byob
        credentialConfiguration.setCredentialConfigurationId(dynamicCredentialConfiguration.credentialConfigurationId());
        credentialConfiguration.setCredentialType(dynamicCredentialConfiguration.credentialType());
        credentialConfiguration.setFormat(CredentialFormat.fromString(dynamicCredentialConfiguration.format()));
        credentialConfiguration.setScope(dynamicCredentialConfiguration.scope());
        credentialConfiguration.setExtendedCredentialMetadata(dynamicCredentialConfiguration.toExtendedCredentialMetadata());
        CredentialIssuerContext credentialIssuerContext = new CredentialIssuerContext();
        // TODO dette kan på sikt styres av konfigurasjonsskilden BYOB selv
        credentialIssuerContext.setCredentialDataSourceUri(URI.create("class://no.idporten.eudiw.issuer.claimssource.byob.ByobClaimsSource"));
        credentialIssuerContext.setValidityDays(30);
        credentialIssuerContext.setGrantType("urn:ietf:params:oauth:grant-type:pre-authorized_code");
        credentialIssuerContext.setAuthorizationServer("auth-eidas2sandkasse");
        credentialIssuerContext.setPreAuthorizationServer("maskinporten");
        credentialIssuerContext.setCredentialSigningKeystore("eaa-provider");
        credentialConfiguration.setCredentialIssuerContext(credentialIssuerContext);
        return credentialConfiguration;
    }

}
