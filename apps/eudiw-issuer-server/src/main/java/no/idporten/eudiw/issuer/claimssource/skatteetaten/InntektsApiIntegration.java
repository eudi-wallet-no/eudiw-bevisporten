package no.idporten.eudiw.issuer.claimssource.skatteetaten;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import io.micrometer.common.util.StringUtils;
import no.idporten.eudiw.issuer.claimssource.advokattilsynet.model.PersonPrivate;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceDataNotFoundException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceException;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceIOException;
import no.idporten.eudiw.issuer.claimssource.skatteetaten.domain.OppgaveInntektsmottaker;
import no.idporten.eudiw.issuer.claimssource.skatteetaten.domain.Respons;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static no.idporten.eudiw.issuer.claimssource.AuthoritativeSource.INNTEKTSAPI;

/**
 * Integration with Skattetaten API inntektsopplysninger for mottaker
 */
@Service
public class InntektsApiIntegration {

    private static final Logger log = LoggerFactory.getLogger(InntektsApiIntegration.class);

    private final InntekstsApiProperties inntekstsApiProperties;
    private final MaskinportenClient maskinportenClient;
    private final RestClient inntekstApiRestClient;

    @Autowired
    public InntektsApiIntegration(InntekstsApiProperties inntekstsApiProperties,
                                  @Qualifier("inntektsApiMaskinportenClient") MaskinportenClient maskinportenClient,
                                  @Qualifier("inntekstApiRestClient") RestClient inntekstApiRestClient) {
        this.inntekstsApiProperties = inntekstsApiProperties;
        this.maskinportenClient = maskinportenClient;
        this.inntekstApiRestClient = inntekstApiRestClient;
    }

    protected AccessToken createAccessToken(String personIdentifier) {
        return maskinportenClient.getAccessToken(personIdentifier, List.of(inntekstsApiProperties.scope()));
    }

    public Respons retrieve(String personIdentifier) {
        try {
            Respons oppgaveInntektsmottaker = inntekstApiRestClient
                    .get()
                    // TODO beregn datoer
                    .uri("v1/lommebok/{personIdentifier}/inntekter?fraOgMed=2025-01&tilOgMed=2025-11", personIdentifier)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(personIdentifier).getValue())
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> handleErrorResponse(response))
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponse(response))
                    .body(Respons.class);
            if (oppgaveInntektsmottaker == null) {
                throw new ClaimsSourceDataNotFoundException(INNTEKTSAPI.name(), "No data available", "Failed to map response");
            }
            return oppgaveInntektsmottaker;
        } catch (ResourceAccessException e) {
            throw new ClaimsSourceIOException(INNTEKTSAPI.name(), "IO error when calling Inntekts-api", e);
        } catch (RestClientException e) {
            throw new ClaimsSourceException(INNTEKTSAPI.name(), "server_error", "Failed to get information from Inntekts-api", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    void handleErrorResponse(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to get data fra authoritative source.  Status: %s, message: %s".formatted(response.getStatusCode(), body);
        throw new ClaimsSourceException(INNTEKTSAPI.name(), "server_error", "Failed to get information from Inntekts-api", HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
    }

}
