package no.idporten.eudiw.issuer.authoritativesources.krr;

import com.nimbusds.oauth2.sdk.token.AccessToken;
import no.idporten.eudiw.issuer.claimssource.exception.ClaimsSourceInvalidDataException;
import no.idporten.eudiw.issuer.authoritativesources.krr.model.PersonKrr;
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
import java.util.Objects;

import static no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource.KRR;
import static org.springframework.util.StringUtils.hasLength;
import static org.springframework.util.StringUtils.hasText;

/**
 * Integration with krr to retrieve info about reservations.
 */
@Service
public class KrrIntegration {

    private static final Logger log = LoggerFactory.getLogger(KrrIntegration.class);


    private final KrrProperties krrProperties;
    private final MaskinportenClient maskinportenClient;
    private final RestClient krrRestClient;

    @Autowired
    public KrrIntegration(KrrProperties krrProperties,
                          @Qualifier("krrMaskinportenClient") MaskinportenClient maskinportenClient,
                          @Qualifier("krrRestClient") RestClient krrRestClient) {
        this.krrProperties = krrProperties;
        this.maskinportenClient = maskinportenClient;
        this.krrRestClient = krrRestClient;
    }

    protected AccessToken createAccessToken(String personIdentifier) {
        return maskinportenClient.getAccessToken(personIdentifier, krrProperties.scopeAsList());
    }

    public PersonKrr retrieve(String personIdentifier) {
        try {
            PersonKrr personKrr = getPersonKrr(personIdentifier);
            if (personKrr == null) {
                throw new ClaimsSourceDataNotFoundException(KRR.name(), "No data available", "Failed to map response");
            } else if (!Objects.equals(personKrr.reservasjon(), "NEI")){
                throw new ClaimsSourceInvalidDataException(KRR.name(), "invalid_request", "The request is not valid" , "Person RESERVED in KRR");
            } else if (!Objects.equals(personKrr.status(), "AKTIV")) {
                throw new ClaimsSourceInvalidDataException(KRR.name(), "invalid_request", "The request is not valid", "Person not ACTIVE in KRR");
            } else if(!Objects.equals(personKrr.varslingsstatus(), "KAN_VARSLES")) {
                throw new ClaimsSourceInvalidDataException(KRR.name(), "invalid_request", "The request is not valid", "Person not verified in KRR");
            } else if(!hasText(personKrr.kontaktinformasjon().epostadresse()) &&
                    !hasText(personKrr.kontaktinformasjon().mobiltelefonnummer())) {
                throw new ClaimsSourceInvalidDataException(KRR.name(), "invalid_request", "The request is not valid", "Person has neither epost nor mobil in KRR");
            }
            return personKrr;
        } catch (ResourceAccessException e) {
            throw new ClaimsSourceIOException(KRR.name(), "IO error when calling KRR", e);
        } catch (RestClientException e) {
            throw new ClaimsSourceException(KRR.name(), "server_error", "Failed to get information from KRR", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    protected PersonKrr getPersonKrr(String personIdentifier) {
        PersonKrr personKrr = krrRestClient
                .get()
                .uri("rest/v2/person")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(personIdentifier).getValue())
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> handleErrorResponse(response))
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> handleErrorResponse(response))
                .body(PersonKrr.class);
        return personKrr;
    }

    void handleErrorResponse(ClientHttpResponse response) throws IOException {
        final String body = StreamUtils.copyToString(response.getBody(), Charset.defaultCharset());
        String logMessage = "Failed to get data fra authoritative source.  Status: %s, message: %s".formatted(response.getStatusCode(), body);
        throw new ClaimsSourceException(KRR.name(), "server_error", "Failed to get information from KRR", HttpStatus.INTERNAL_SERVER_ERROR, logMessage);
    }

}
