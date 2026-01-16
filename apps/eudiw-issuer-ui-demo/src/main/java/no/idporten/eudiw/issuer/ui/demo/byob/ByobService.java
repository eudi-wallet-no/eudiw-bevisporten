package no.idporten.eudiw.issuer.ui.demo.byob;

import no.idporten.eudiw.issuer.ui.demo.byob.model.CredentialDefinition;
import no.idporten.eudiw.issuer.ui.demo.byob.model.CredentialDefinitionCollection;
import no.idporten.eudiw.issuer.ui.demo.exception.ByobServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class ByobService {
    private final ByobServiceProperties byobServiceProperties;
    private final RestClient restClient;

   @Autowired
    public ByobService(
            ByobServiceProperties byobServiceProperties,
            @Qualifier("byobServiceRestClient")
            RestClient restClient
   ) {
        this.byobServiceProperties = byobServiceProperties;
        this.restClient = restClient;
   }

    public List<CredentialDefinition> getCredentialConfigurations() {
        List<CredentialDefinition> result = getCertificationDefinitionCollection().credentialConfigurations;
        if (result == null) {
            result = new ArrayList<>();
        }
        return result;
    }

    public CredentialDefinition addCredentialDefinition(CredentialDefinition credentialDefinition) {
        return storeCredential(credentialDefinition);
    }

    public CredentialDefinition getCredentialDefinitionByVct(String vct) {
        return getDefinitionByCvt(vct);
    }

    public void removeCustomCredentialDefinition(String vct) {
        deleteCredentialDefinition(vct);
    }

    public boolean existsByVct(String vct) {
        return getCredentialConfigurations().stream().anyMatch(c -> c.getVct().equals(vct));
    }

    public CredentialDefinition editCredentialDefinition(CredentialDefinition cd) {
       return putCredentialDefinition(cd);
    }

     private CredentialDefinition getDefinitionByCvt(String vct) {
        String endpoint = byobServiceProperties.getEndpoint();
        URI uri = UriComponentsBuilder.fromUriString(endpoint).path("/%s".formatted(vct)).build().toUri();

        try {
            return restClient
                    .get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(CredentialDefinition.class);
        } catch (RestClientResponseException e) {
            throw new ByobServiceException("Configuration error against byob-service? path=" + endpoint, e);
        }
    }

    private CredentialDefinitionCollection getCertificationDefinitionCollection() {
       String getEndpoint = byobServiceProperties.getManyEndpoint();

       try {
          return restClient
                  .get()
                  .uri(getEndpoint)
                  .accept(MediaType.APPLICATION_JSON)
                  .retrieve()
                  .body(CredentialDefinitionCollection.class);
        } catch (RestClientResponseException e) {
            throw new ByobServiceException("Configuration error against byob-service? path=" + getEndpoint, e);
        }
    }

    private CredentialDefinition storeCredential(CredentialDefinition cd) {
        String persistEndpoint = byobServiceProperties.createEndpoint();

        try {
            return restClient
                    .post()
                    .uri(persistEndpoint)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cd)
                    .retrieve()
                    .body(CredentialDefinition.class);
        } catch (RestClientResponseException e) {
            throw new ByobServiceException("Configuration error against byob-service? path=" + persistEndpoint, e);
        }
    }

    private CredentialDefinition putCredentialDefinition(CredentialDefinition cd) {
        String endpoint = byobServiceProperties.getEndpoint();
        URI uri = UriComponentsBuilder.fromUriString(endpoint).build().toUri();

        try {
            return restClient
                    .put()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cd)
                    .retrieve()
                    .body(CredentialDefinition.class);
        }
        catch (RestClientResponseException e) {
            throw new ByobServiceException(e.getMessage(), e);
        }
    }

    private void deleteCredentialDefinition(String vct) {
        String endpoint = byobServiceProperties.getEndpoint();
        URI uri = UriComponentsBuilder.fromUriString(endpoint).queryParam("vct", vct).build().toUri();

        try {
            restClient
                    .delete()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new ByobServiceException("Configuration error against byob-service? path=" + endpoint, e);
        }
    }
}
