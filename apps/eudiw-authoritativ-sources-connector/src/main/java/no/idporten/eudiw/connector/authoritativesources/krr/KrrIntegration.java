package no.idporten.eudiw.connector.authoritativesources.krr;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceException;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceIOException;
import no.idporten.eudiw.connector.authoritativesources.krr.model.PersonKrr;
import no.idporten.lib.maskinporten.client.MaskinportenClient;
import no.idporten.lib.maskinporten.client.MaskinportenClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.Charset;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.KRR;


/**
 * Integration with krr to retrieve info about reservations.
 */
@Service
public class KrrIntegration {

    private static final Logger log = LoggerFactory.getLogger(KrrIntegration.class);


    private final KrrProperties krrProperties;
    private final MaskinportenClient maskinportenClient;
    private final RestClient krrRestClient;

    public KrrIntegration(KrrProperties krrProperties,
                         MaskinportenClients maskinportenClients,
                          @Qualifier("krrRestClient") RestClient krrRestClient) {
        this.krrProperties = krrProperties;
        this.maskinportenClient = maskinportenClients.getClient(krrProperties.maskinportenClient());
        this.krrRestClient = krrRestClient;
    }

    protected AccessToken createAccessToken(String personIdentifier) {
        return maskinportenClient.getAccessToken(personIdentifier, krrProperties.scopeAsList());
    }

    public PersonKrr retrieve(String personIdentifier) {
        try {
           return getPersonKrr(personIdentifier);
        } catch (ResourceAccessException e) {
            throw new AuthoritativeSourceIOException(KRR, "IO error when calling KRR", e);
        } catch (RestClientException e) {
            throw new AuthoritativeSourceException(KRR, "server_error", "Failed to get information from KRR", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    protected PersonKrr getPersonKrr(String personIdentifier) {
        return krrRestClient
                .get()
                .uri("rest/v2/person")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(personIdentifier).getValue())
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> handleErrorResponse(response))
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponse(response))
                .body(PersonKrr.class);
    }

    void handleErrorResponse(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to get data fra authoritative source.  Status: %s, message: %s".formatted(response.getStatusCode(), body);
        throw new AuthoritativeSourceException(KRR, "server_error", "Failed to get information from KRR", HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
    }

}
