package no.idporten.eudiw.issuer.claimssource.advokattilsynet;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import no.idporten.eudiw.issuer.claimssource.advokattilsynet.model.PersonPrivate;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceIOException;
import no.idporten.lib.maskinporten.client.MaskinportenClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static no.idporten.eudiw.issuer.claimssource.AuthoritativeSource.ADVOKATREGISTERET;

/**
 * Integration with data.altinn.no to rertrieve info from Advokatregisteret.
 */
@Service
public class AdvokatregisteretIntegration {

    private static final Logger log = LoggerFactory.getLogger(AdvokatregisteretIntegration.class);
    public static final String SUBSCRIPTION_KEY_HEADER = "Ocp-apim-subscription-key";

    private final AdvokatregisteretProperties advokatregisteretProperties;
    private final MaskinportenClient maskinportenClient;
    private final RestClient advokatregisterRestClient;

    @Autowired
    public AdvokatregisteretIntegration(AdvokatregisteretProperties advokatregisteretProperties,
                                        MaskinportenClient maskinportenClient,
                                        @Qualifier("advokatregisterRestClient") RestClient advokatregisterRestClient) {
        this.advokatregisteretProperties = advokatregisteretProperties;
        this.maskinportenClient = maskinportenClient;
        this.advokatregisterRestClient = advokatregisterRestClient;
    }

    protected AccessToken createAccessToken(String personIdentifier) {
        return maskinportenClient.getAccessToken(List.of(advokatregisteretProperties.scope()), personIdentifier);
    }

    public PersonPrivate retrieve(String personIdentifier) {
        try {
            return advokatregisterRestClient
                    .get()
                    .uri("?subject={personIdentifier}&envelope=false", personIdentifier)
                    .header(SUBSCRIPTION_KEY_HEADER, advokatregisteretProperties.subscriptionKey())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(personIdentifier).getValue())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> handleErrorResponse(response))
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponse(response))
                    .body(PersonPrivate.class);
        } catch (ResourceAccessException e) {
            throw new ClaimsSourceIOException(ADVOKATREGISTERET.name(), "IO error when calling Advokatregisteret with subject", e);
        }
    }


    void handleErrorResponse(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to get data fra authoritative source.  Status: %s, message: %s".formatted(response.getStatusCode(), body);
        throw new ClaimsSourceException(ADVOKATREGISTERET.name(), "server_error", "Failed to get information from Advokatregisteret", HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
    }

}
