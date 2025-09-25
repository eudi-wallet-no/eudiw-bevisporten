package no.idporten.eudiw.issuer.ui.demo.issuer;

import no.idporten.eudiw.issuer.ui.demo.exception.IssuerServerException;
import no.idporten.eudiw.issuer.ui.demo.exception.IssuerUiException;
import no.idporten.eudiw.issuer.ui.demo.issuer.config.CredentialConfiguration;
import no.idporten.eudiw.issuer.ui.demo.issuer.config.IssuerServerProperties;
import no.idporten.eudiw.issuer.ui.demo.issuer.domain.IssuanceResponse;
import no.idporten.eudiw.issuer.ui.demo.web.StartIssuanceForm;
import no.idporten.lib.maskinporten.client.UserBoundJwtHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class IssuerServerService {

    private final static Logger log = LoggerFactory.getLogger(IssuerServerService.class);

    private final IssuerServerProperties issuerServerProperties;
    private final RestClient restClient;
    private final UserBoundJwtHelper userBoundJwtHelper;

    @Autowired
    public IssuerServerService(@Qualifier("issuerServerRestClient") RestClient restClient,
                               IssuerServerProperties issuerServerProperties,
                               UserBoundJwtHelper userBoundJwtHelper) {
        this.issuerServerProperties = issuerServerProperties;
        this.restClient = restClient;
        this.userBoundJwtHelper = userBoundJwtHelper;
    }

    private String createAccessToken(CredentialConfiguration credentialConfiguration, StartIssuanceForm startIssuanceForm) {
        return userBoundJwtHelper.getAccessTokenValue(
                List.of(credentialConfiguration.scope()),
                StringUtils.hasText(startIssuanceForm.personIdentifier()) ? startIssuanceForm.personIdentifier() : null);
    }

    public IssuanceResponse startIssuance(CredentialConfiguration credentialConfiguration, StartIssuanceForm json) {
        String issuanceEndpoint = issuerServerProperties.getIssuanceEndpoint();
        String accessToken = createAccessToken(credentialConfiguration, json);
        IssuanceResponse result;
        try {
            result = restClient.post().uri(
                            issuanceEndpoint)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(accessToken))
                    .body(json.json())
                    .retrieve()
                    .body(IssuanceResponse.class);
        } catch (HttpClientErrorException e) {
            throw new IssuerServerException("Configuration error against issuer-server? path=" + issuanceEndpoint, e);
        } catch (HttpServerErrorException e) {
            throw new IssuerServerException("callIssuerServer failed for input" + json, e);
        }
        if (result == null || result.credentialOffer() == null) {
            throw new IssuerUiException("callIssuerServer returned null for input: " + json);
        }
        log.debug("Searched for " + json + ". Returned: " + result);
        return result;
    }

}
