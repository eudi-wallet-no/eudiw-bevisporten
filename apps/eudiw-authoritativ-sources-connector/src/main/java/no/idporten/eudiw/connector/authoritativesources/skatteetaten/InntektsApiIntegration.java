package no.idporten.eudiw.connector.authoritativesources.skatteetaten;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import no.idporten.eudiw.connector.authoritativesources.exceptions.AuthoritativeSourceException;
import no.idporten.eudiw.connector.authoritativesources.exceptions.ClaimsSourceIOException;
import no.idporten.eudiw.connector.authoritativesources.skatteetaten.domain.Respons;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static no.idporten.eudiw.connector.authoritativesources.AuthoritativeSources.SKATTEETATEN;


/**
 * Integration with Skattetaten API inntektsopplysninger for mottaker
 */
@Service
public class InntektsApiIntegration {

    private static final Logger log = LoggerFactory.getLogger(InntektsApiIntegration.class);

    private final InntekstsApiProperties inntekstsApiProperties;
    private final MaskinportenClient maskinportenClient;
    private final RestClient inntekstApiRestClient;

    public InntektsApiIntegration(InntekstsApiProperties inntekstsApiProperties,
                                  MaskinportenClients maskinportenClients,
                                  @Qualifier("inntekstApiRestClient") RestClient inntekstApiRestClient) {
        this.inntekstsApiProperties = inntekstsApiProperties;
        this.maskinportenClient = maskinportenClients.getClient(inntekstsApiProperties.maskinportenClient());
        this.inntekstApiRestClient = inntekstApiRestClient;
    }

    protected AccessToken createAccessToken(String personIdentifier) {
        return maskinportenClient.getAccessToken(personIdentifier, List.of(inntekstsApiProperties.scope()));
    }

    public Respons retrieve(String personIdentifier, LocalDate startDate, LocalDate endDate) {
        try {
            return getRespons(personIdentifier, startDate, endDate);
        } catch (ResourceAccessException e) {
            throw new ClaimsSourceIOException(SKATTEETATEN, "IO error when calling Inntekts-api", e);
        } catch (RestClientException e) {
            throw new AuthoritativeSourceException(SKATTEETATEN, "server_error", "Failed to get information from Inntekts-api", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private Respons getRespons(String personIdentifier, LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter yyyyMMFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return inntekstApiRestClient
                .get()
                .uri("v1/lommebok/{personIdentifier}/inntekter?fraOgMed={from}&tilOgMed={to}",
                        personIdentifier,
                        startDate.format(yyyyMMFormatter),
                        endDate.format(yyyyMMFormatter))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(personIdentifier).getValue())
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> handleErrorResponse(response))
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponse(response))
                .body(Respons.class);
    }

    void handleErrorResponse(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to get data fra authoritative source.  Status: %s, message: %s".formatted(response.getStatusCode(), body);
        throw new AuthoritativeSourceException(SKATTEETATEN, "server_error", "Failed to get information from Inntekts-api", HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
    }

}
