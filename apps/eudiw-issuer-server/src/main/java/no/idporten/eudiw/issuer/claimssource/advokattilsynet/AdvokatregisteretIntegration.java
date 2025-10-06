package no.idporten.eudiw.issuer.claimssource.advokattilsynet;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import io.micrometer.common.util.StringUtils;
import no.idporten.eudiw.issuer.claimssource.advokattilsynet.model.PersonPrivate;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceDataNotFoundException;
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
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static no.idporten.eudiw.issuer.claimssource.AuthoritativeSource.ADVOKATREGISTERET;

/**
 * Integration with data.altinn.no to retrieve info from Advokatregisteret.
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
                                        @Qualifier("advokatregisteretMaskinportenClient") MaskinportenClient maskinportenClient,
                                        @Qualifier("advokatregisteretRestClient") RestClient advokatregisterRestClient) {
        this.advokatregisteretProperties = advokatregisteretProperties;
        this.maskinportenClient = maskinportenClient;
        this.advokatregisterRestClient = advokatregisterRestClient;
    }

    protected AccessToken createAccessToken(String personIdentifier) {
        return maskinportenClient.getAccessToken(personIdentifier, List.of(advokatregisteretProperties.scope()));
    }

    public PersonPrivate retrieve(String personIdentifier) {
        try {
            PersonPrivate personPrivate = advokatregisterRestClient
                    .get()
                    .uri("?subject={personIdentifier}&envelope=false", personIdentifier)
                    .header(SUBSCRIPTION_KEY_HEADER, advokatregisteretProperties.subscriptionKey())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(personIdentifier).getValue())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> handleErrorResponse(response))
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponse(response))
                    .body(PersonPrivate.class);
            if (personPrivate == null) {
                throw new ClaimsSourceDataNotFoundException(ADVOKATREGISTERET.name(), "No data available", "Failed to map response");
            }
            if (StringUtils.isEmpty(personPrivate.tittel())) {
                throw new ClaimsSourceDataNotFoundException(ADVOKATREGISTERET.name(), "No data available", "No value for title, assuming empty response");
            }
            return personPrivate;
        } catch (ResourceAccessException e) {
            throw new ClaimsSourceIOException(ADVOKATREGISTERET.name(), "IO error when calling Advokatregisteret", e);
        } catch (RestClientException e) {
            throw new ClaimsSourceException(ADVOKATREGISTERET.name(), "server_error", "Failed to get information from Advokatregisteret", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    void handleErrorResponse(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to get data fra authoritative source.  Status: %s, message: %s".formatted(response.getStatusCode(), body);
        throw new ClaimsSourceException(ADVOKATREGISTERET.name(), "server_error", "Failed to get information from Advokatregisteret", HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
    }

}
