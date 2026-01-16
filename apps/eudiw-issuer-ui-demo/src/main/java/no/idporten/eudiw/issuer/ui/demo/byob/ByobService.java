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

    public List<CredentialDefinition> getCustomCredentialDefinitions() {
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

    public List<CredentialDefinition> getCredentialConfigurations() {
        return  getCustomCredentialDefinitions();
    }

    public boolean existsByVct(String vct) {
        return getCustomCredentialDefinitions().stream().anyMatch(c -> c.getVct().equals(vct));
    }

    private CredentialDefinition getDefinitionByCvt(String vct) {
        String endpoint = byobServiceProperties.getEndpoint();
        URI uri = URI.create(endpoint + "/" + vct);

        CredentialDefinition result;
        try {
            result = restClient
                    .get()
                    .uri(uri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(CredentialDefinition.class);
        } catch (RestClientResponseException e) {
            throw new ByobServiceException("Configuration error against byob-service? path=" + endpoint, e);
        }

        return result;
    }

    private CredentialDefinitionCollection getCertificationDefinitionCollection() {
       String getEndpoint = byobServiceProperties.getManyEndpoint();

       CredentialDefinitionCollection result;

       try {
          result = restClient
                  .get()
                  .uri(getEndpoint)
                  .accept(MediaType.APPLICATION_JSON)
                  .retrieve()
                  .body(CredentialDefinitionCollection.class);
        } catch (RestClientResponseException e) {
            throw new ByobServiceException("Configuration error against byob-service? path=" + getEndpoint, e);
        }

       return result;
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

    private void deleteCredentialDefinition(String vct) {
        String endpoint = byobServiceProperties.getEndpoint();
        URI uri = URI.create(endpoint + "?vct=" + vct);

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
