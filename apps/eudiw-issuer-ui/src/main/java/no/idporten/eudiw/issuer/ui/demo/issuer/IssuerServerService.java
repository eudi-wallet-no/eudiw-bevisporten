package no.idporten.eudiw.issuer.ui.demo.issuer;

import no.idporten.eudiw.issuer.ui.demo.exception.IssuerServerException;
import no.idporten.eudiw.issuer.ui.demo.exception.IssuerUiException;
import no.idporten.eudiw.issuer.ui.demo.issuer.config.IssuerServerProperties;
import no.idporten.eudiw.issuer.ui.demo.issuer.domain.IssuanceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@Service
public class IssuerServerService {

    private final IssuerServerProperties issuerServerProperties;

    private final RestClient restClient;

    private final Logger log = LoggerFactory.getLogger(IssuerServerService.class);

    @Autowired
    public IssuerServerService(@Qualifier("issuerServerRestClient") RestClient restClient, IssuerServerProperties issuerServerProperties) {
        this.issuerServerProperties = issuerServerProperties;
        this.restClient = restClient;
    }

    public IssuanceResponse startIssuance(String json) {
        String issuanceEndpoint = issuerServerProperties.getIssuanceEndpoint();
        IssuanceResponse result;
        try {
            result = restClient.post().uri(
                            issuanceEndpoint).accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).body(json).retrieve()
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
