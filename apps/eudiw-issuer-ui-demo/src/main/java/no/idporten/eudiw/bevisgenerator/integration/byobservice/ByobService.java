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

    public List<CredentialDefinition> getCredentialDefinitionsForEdit() {
        List<CredentialDefinition> result = getCertificationDefinitionCollection(byobServiceProperties.getAllEditEndpoint()).credentialConfigurations;
        if (result == null) {
            result = new ArrayList<>();
        }
        return result;
    }

    public List<CredentialDefinition> getCredentialDefinitionsForIssuance() {
        List<CredentialDefinition> result = getCertificationDefinitionCollection(byobServiceProperties.getAllIssueEndpoint()).credentialConfigurations;
        if (result == null) {
            result = new ArrayList<>();
        }
        return result;
    }

    public CredentialDefinition addCredentialDefinition(CredentialDefinition credentialDefinition) {
        return storeCredential(credentialDefinition);
    }

    public CredentialDefinition getByCredentialType(String credentialType) {
        return getDefinitionByCredentialType(credentialType);
    }

    public CredentialDefinition getByCredentialConfigurationId(String credentialConfigurationId) {
        return getDefinitionById(credentialConfigurationId);
    }

    public void removeCustomCredentialDefinition(String credentialType) {
        deleteCredentialDefinition(credentialType);
    }

    public boolean existsByCredentialType(String credentialType) {
        return getCredentialDefinitionsForEdit().stream().anyMatch(c -> c.getCredentialType().equals(credentialType));
    }

    public CredentialDefinition editCredentialDefinition(CredentialDefinition cd) {
        return putCredentialDefinition(cd);
    }

    private CredentialDefinition getDefinitionById(String id) {
        return getCredentialDefinitionsForIssuance()
                .stream()
                .filter(cd -> Objects.equals(cd.getCredentialConfigurationId(), id))
                .findFirst()
                .orElse(null);
    }

    private CredentialDefinition getDefinitionByCredentialType(String credentialType) {
        String endpoint = byobServiceProperties.getEndpoint();
        URI uri = UriComponentsBuilder.fromUriString(endpoint).path("/%s".formatted(credentialType)).build().toUri();

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

    private CredentialDefinitionCollection getCertificationDefinitionCollection(String endpoint) {
        try {
            return restClient
                    .get()
                    .uri(endpoint)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(CredentialDefinitionCollection.class);
        } catch (ResourceAccessException e) {
            throw new ByobServiceIOException("IO error when calling BYOB service to store CredentialDefinition", e);
        } catch (RestClientException e) {
            throw new ByobServiceException("Configuration error against byob-service? path=" + endpoint, e);
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

    private void deleteCredentialDefinition(String credentialType) {
        String endpoint = byobServiceProperties.getEndpoint();
        URI uri = UriComponentsBuilder.fromUriString(endpoint).queryParam("credential-type", credentialType).build().toUri();

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
