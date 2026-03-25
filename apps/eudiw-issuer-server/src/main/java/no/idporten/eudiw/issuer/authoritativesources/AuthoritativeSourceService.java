package no.idporten.eudiw.issuer.authoritativesources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.config.AuthoritativeSourceProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.openid4vci.protocol.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for retrieving credential data from external authoritative sources.
 */
@Service
public class AuthoritativeSourceService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AuthoritativeSourceService.class);

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final Map<String, RestClient> restClients = new HashMap<>();

    public AuthoritativeSourceService(CredentialIssuerServerProperties credentialIssuerServerProperties) {
        this.credentialIssuerServerProperties = credentialIssuerServerProperties;
    }

    private AuthoritativeSourceProperties findAuthoritativeSourceProperties(String source) {
        return Optional.ofNullable(credentialIssuerServerProperties.getAuthoritativeSources().get(source))
                .orElseThrow(() -> new IssuerServerException(
                        "server_error",
                        "Unknown authoritative source",
                        "Unknown authoritative source [%s]".formatted(source),
                        HttpStatus.INTERNAL_SERVER_ERROR)
                );
    }

    public CredentialData retrieveCredentialData(String source, String credentialType, String personIdentifier) {
        Subject subject = new Subject(personIdentifier);
        AuthoritativeSourceRequest authoritativeSourceRequest = new AuthoritativeSourceRequest(subject, credentialType);
        AuthoritativeSourceProperties authoritativeSourceProperties = findAuthoritativeSourceProperties(source);
        AuthoritativeSourceResponse authoritativeSourceResponse = restClients.get(source)
                .post()
                .uri(authoritativeSourceProperties.uri())
                .body(authoritativeSourceRequest)
                .retrieve()
                .body(AuthoritativeSourceResponse.class);
        // TODO feilhåndtering egen sak
        return new CredentialData(authoritativeSourceResponse.credentialData());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (Map.Entry<String, AuthoritativeSourceProperties> entry : credentialIssuerServerProperties.getAuthoritativeSources().entrySet()) {
            restClients.put(entry.getKey(), createRestClient(entry.getValue()));
            log.info("Initialized authoritative source {} with URI {}", entry.getKey(), entry.getValue().uri());
        }
    }

    private RestClient createRestClient(AuthoritativeSourceProperties properties) {
        SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        clientHttpRequestFactory.setConnectTimeout(properties.connectTimeout());
        clientHttpRequestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    record AuthoritativeSourceRequest(
            @JsonProperty("subject") Subject subject,
            @JsonProperty("credential_type") String credentialType
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AuthoritativeSourceResponse(
            @JsonProperty("credential_data") Map<String, Object> credentialData
    ) {
    }

}
