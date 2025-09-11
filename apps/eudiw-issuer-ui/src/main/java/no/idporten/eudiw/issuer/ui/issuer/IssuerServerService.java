package no.idporten.eudiw.issuer.ui.issuer;

import no.idporten.eudiw.issuer.ui.exception.IssuerServerException;
import no.idporten.eudiw.issuer.ui.exception.IssuerUiException;
import no.idporten.eudiw.issuer.ui.issuer.config.IssuerServerProperties;
import no.idporten.eudiw.issuer.ui.issuer.domain.CredentialOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

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

    public CredentialOffer startIssuance(String credentialConfigurationId) {
        log.info("Starting issuance for credential configuration {}", credentialConfigurationId);
        String issuanceCreatePath = issuerServerProperties.getIssuanceEndpoint();
        String uri = UriComponentsBuilder.fromPath(issuanceCreatePath)
                .queryParam("credential_configuration_id", credentialConfigurationId)
                .toUriString();
        CredentialOffer result;
        try {
            result = restClient.get().uri(
                            uri).accept(MediaType.APPLICATION_JSON).retrieve()
                    .body(CredentialOffer.class);
        } catch (HttpClientErrorException e) {
            throw new IssuerServerException("Configuration error against issuer-server? uri=" + uri, e);
        } catch (HttpServerErrorException e) {
            throw new IssuerServerException("call IssuerServer failed for input" + credentialConfigurationId, e);
        }
        if (result == null) {
            throw new IssuerUiException("callIssuerServer returned null for input: " + credentialConfigurationId);
        }
        log.debug("Searched for " + credentialConfigurationId + ". Returned: " + result);
        return result;
    }

}
