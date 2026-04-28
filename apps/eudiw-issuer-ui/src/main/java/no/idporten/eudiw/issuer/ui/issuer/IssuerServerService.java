package no.idporten.eudiw.issuer.ui.issuer;

import no.idporten.eudiw.issuer.ui.exception.IssuerServerException;
import no.idporten.eudiw.issuer.ui.exception.IssuerUiException;
import no.idporten.eudiw.issuer.ui.issuer.config.CredentialConfiguration;
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

import java.net.URI;

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

    public URI createCredentialOfferRequestUri(CredentialConfiguration credentialConfiguration) {
        return UriComponentsBuilder
                .fromUriString(credentialConfiguration.issuer())
                .path(issuerServerProperties.getIssuanceEndpoint())
                .queryParam("credential_configuration_id", credentialConfiguration.id())
                .build()
                .toUri();
    }

    public CredentialOffer startIssuance(CredentialConfiguration credentialConfiguration, URI uri) {
        log.info("Starting issuance for credential configuration {}", credentialConfiguration.id());
        CredentialOffer result;
        try {
            result = restClient.get().uri(
                            uri).accept(MediaType.APPLICATION_JSON).retrieve()
                    .body(CredentialOffer.class);
        } catch (HttpClientErrorException e) {
            throw new IssuerServerException("Configuration error against issuer-server? uri=" + uri, e);
        } catch (HttpServerErrorException e) {
            throw new IssuerServerException("call IssuerServer failed for input" + credentialConfiguration.id(), e);
        }
        if (result == null) {
            throw new IssuerUiException("callIssuerServer returned null for input: " + credentialConfiguration.id());
        }
        log.debug("Searched for " + credentialConfiguration.id() + ". Returned: " + result);
        return result;
    }

}
