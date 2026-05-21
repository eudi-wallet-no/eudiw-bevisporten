package no.idporten.eudiw.issuer.config;

import no.idporten.eudiw.issuer.credentials.configurations.*;
import no.idporten.eudiw.issuer.credentials.formats.CredentialFormat;
import no.idporten.eudiw.issuer.credentials.types.ClaimDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
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

    private final APIConnectionProperties apiConnectionProperties;

    private RestClient restClient;

    private final CopyOnWriteArrayList<ExtendedCredentialConfiguration> credentialConfigurations = new CopyOnWriteArrayList<>();

    public HttpCredentialConfigurationSource(APIConnectionProperties apiConnectionProperties) {
        this.apiConnectionProperties = apiConnectionProperties;
        this.restClient = apiConnectionProperties.createRestClient();
    }

    /**
     * Initializes credential configuration source by refreshing.
     */
    @Override
    public void init() {
        refresh();
    }

    @Override
    public List<ExtendedCredentialConfiguration> retrieve() {
        return credentialConfigurations;
    }

    /**
     * Refreshes credential configurations by calling external service. Clears existing configurations and replaces with new ones.
     */
    @Override
    public void refresh() {
        try {
            ExtendedCredentialConfigurations credentialConfigurations = restClient
                    .get()
                    .uri(apiConnectionProperties.uri())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> handleErrorResponse(response))
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponse(response))
                    .body(ExtendedCredentialConfigurations.class);

            if (credentialConfigurations == null || credentialConfigurations.getCredentialConfigurations() == null || credentialConfigurations.getCredentialConfigurations().isEmpty()) {
                log.warn("No credential configurations retrieved from {}", apiConnectionProperties.uri());
                return;
            }
            log.info("Retrieved {} credential-configurations from {}", credentialConfigurations.getCredentialConfigurations().size(), apiConnectionProperties.uri());
            List<ExtendedCredentialConfiguration> extendedCredentialConfigurations = credentialConfigurations.getCredentialConfigurations().stream().map(this::fixByobCredentialConfiguration).toList();
            this.credentialConfigurations.clear();
            this.credentialConfigurations.addAll(extendedCredentialConfigurations);
        } catch (ResourceAccessException e) {
            throw new CredentialConfigurationSourceException("BYOB", "IO error when calling credential configuration source", e);
        } catch (RestClientException e) {
            throw new CredentialConfigurationSourceException("BYOB", "Failed to get information from credential configuration source", e);
        }
    }

    void handleErrorResponse(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to get data from authoritative source. Status: %s, message: %s".formatted(response.getStatusCode(), body);
        throw new CredentialConfigurationSourceException("BYOB", "Failed to get information from credential configuration source", logMessage);
    }

    public APIConnectionProperties getApiConnectionProperties() {
        return apiConnectionProperties;
    }

    protected ExtendedCredentialConfiguration fixByobCredentialConfiguration(ExtendedCredentialConfiguration credentialConfiguration) {
        // TODO dette kan på sikt styres av konfigurasjonsskilden BYOB selv
        credentialConfiguration.setExtendedCredentialMetadata(fixCredentialMetadata(credentialConfiguration));
        CredentialIssuerContext credentialIssuerContext = new CredentialIssuerContext();
        credentialIssuerContext.setCredentialDataSourceUri(URI.create("class://no.idporten.eudiw.issuer.claimssource.PushPreAuthorizedClaimsSource"));
        credentialIssuerContext.setValidityDays(30);
        credentialIssuerContext.setGrantType("urn:ietf:params:oauth:grant-type:pre-authorized_code");
        credentialIssuerContext.setAuthorizationServer("auth-eidas2sandkasse");
        credentialIssuerContext.setPreAuthorizationServer("maskinporten");
        credentialIssuerContext.setCredentialSigningKeystore("eaa-provider");
        credentialIssuerContext.setIncludeStatus(true);
        credentialConfiguration.setCredentialIssuerContext(credentialIssuerContext);
        return credentialConfiguration;
    }

    private ExtendedCredentialMetadata fixCredentialMetadata(ExtendedCredentialConfiguration credentialConfiguration) {
        List<ExtendedClaimsDescription> claims = credentialConfiguration.getExtendedCredentialMetadata().claims().stream()
                .map(claim -> {
                    final ClaimDataType type = fixType(claim.type());
                    return new ExtendedClaimsDescription(
                            fixPath(claim.path(), credentialConfiguration.getFormat(), credentialConfiguration.getCredentialType()),
                            type,
                            claim.mimeType(),
                            claim.display(),
                            claim.mandatory(),
                            fixValidationRegex(claim.validationRegex(), type));
                })
                .toList();
        return new ExtendedCredentialMetadata(credentialConfiguration.getExtendedCredentialMetadata().display(), claims);
    }

    private String fixValidationRegex(String validationRegex, ClaimDataType type) {
        return validationRegex != null ? validationRegex : type.getDefaultRegex();
    }

    private ClaimDataType fixType(ClaimDataType type) {
        if (type == null) {
            return ClaimDataType.STRING;
        }
        return type;
    }

    private List<String> fixPath(List<String> path, CredentialFormat credentialFormat, String doctype) {
        if (credentialFormat == CredentialFormat.SD_JWT_VC) {
            return path;
        }
        return List.of(doctype, path.getFirst());
    }

    protected void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

}
