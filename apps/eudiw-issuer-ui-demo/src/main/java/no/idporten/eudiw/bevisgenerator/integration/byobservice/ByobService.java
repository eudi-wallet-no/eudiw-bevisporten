package no.idporten.eudiw.bevisgenerator.integration.byobservice;

import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.CredentialDefinition;
import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.CredentialDefinitionCollection;
import no.idporten.eudiw.bevisgenerator.exception.ByobServiceException;
import no.idporten.eudiw.bevisgenerator.exception.ByobServiceIOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public List<CredentialDefinition> getCredentialDefinitions() {
        List<CredentialDefinition> result = getCertificationDefinitionCollection().credentialConfigurations;
        if (result == null) {
            result = new ArrayList<>();
        }
        return result;
    }

    public CredentialDefinition addCredentialDefinition(CredentialDefinition credentialDefinition) {
        return storeCredential(credentialDefinition);
    }

    public CredentialDefinition getByVct(String vct) {
        return getDefinitionByCvt(vct);
    }

    public CredentialDefinition getByCredentialConfigurationId(String credentialConfigurationId) {
        return getDefinitionById(credentialConfigurationId);
    }

    public void removeCustomCredentialDefinition(String vct) {
        deleteCredentialDefinition(vct);
    }

    public boolean existsByVct(String vct) {
        return getCredentialDefinitions().stream().anyMatch(c -> c.getVct().equals(vct));
    }

    public CredentialDefinition editCredentialDefinition(CredentialDefinition cd) {
        return putCredentialDefinition(cd);
    }

    private CredentialDefinition getDefinitionById(String id) {
        return getCredentialDefinitions()
                .stream()
                .filter(cd -> Objects.equals(cd.getCredentialConfigurationId(), id))
                .findFirst()
                .orElse(null);
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
        } catch (ResourceAccessException e) {
            throw new ByobServiceIOException("IO error when calling BYOB service to store CredentialDefinition", e);
        } catch (RestClientException e) {
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
        } catch (ResourceAccessException e) {
            throw new ByobServiceIOException("IO error when calling BYOB service to store CredentialDefinition", e);
        } catch (RestClientException e) {
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
        } catch (ResourceAccessException e) {
            throw new ByobServiceIOException("IO error when calling BYOB service to store CredentialDefinition", e);
        } catch (RestClientException e) {
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
        } catch (ResourceAccessException e) {
            throw new ByobServiceIOException("IO error when calling BYOB service to add CredentialDefinition", e);
        } catch (RestClientException e) {
            throw new ByobServiceException("Error when calling BYOB service to add CredentialDefinition", e);
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

        } catch (ResourceAccessException e) {
            throw new ByobServiceIOException("IO error when calling BYOB service to delete CredentialDefinition", e);
        } catch (RestClientException e) {
            throw new ByobServiceException("Configuration error against byob-service on delete? path=" + endpoint, e);
        }
    }
}
