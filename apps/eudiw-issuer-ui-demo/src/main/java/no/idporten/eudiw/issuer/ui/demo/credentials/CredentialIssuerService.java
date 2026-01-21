package no.idporten.eudiw.issuer.ui.demo.credentials;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.issuer.ui.demo.byob.ByobService;
import no.idporten.eudiw.issuer.ui.demo.byob.model.CredentialDefinition;
import no.idporten.eudiw.issuer.ui.demo.byob.model.Display;
import no.idporten.eudiw.issuer.ui.demo.byob.model.ExampleCredentialData;
import no.idporten.eudiw.issuer.ui.demo.exception.IssuerUiException;
import no.idporten.eudiw.issuer.ui.demo.issuer.config.CredentialConfiguration;
import no.idporten.eudiw.issuer.ui.demo.issuer.model.IssuanceClaim;
import no.idporten.eudiw.issuer.ui.demo.issuer.model.IssuanceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CredentialIssuerService {
    private final ByobService byobService;
    private final ObjectMapper objectMapper;
    private final Logger log = LoggerFactory.getLogger(CredentialIssuerService.class);

    @Autowired
    public CredentialIssuerService(ByobService byobService, ObjectMapper objectMapper) {
        this.byobService = byobService;
        this.objectMapper = objectMapper;
    }

    public List<CredentialConfiguration> getCredentialConfigurations() {
        return getCustomCredentials();
    }

    public CredentialConfiguration getCredentialConfigurationById(String id) {
        CredentialDefinition cd = byobService.getByCredentialConfigurationId(id);
        return convertCredentialDefinitionCredentials(cd);
    }

    private List<CredentialConfiguration> getCustomCredentials() {
        return byobService.getCredentialDefinitions()
                .stream()
                .map(this::convertCredentialDefinitionCredentials)
                .toList();
    }

    private CredentialConfiguration convertCredentialDefinitionCredentials(CredentialDefinition cd) {

        // TODO: Hent personIdentifier fra application.yaml
        String personIdentifier = "50917500484";

        IssuanceDefinition data = convertFromCredentialDefinitionToIssuanceDefinition(cd, personIdentifier);
        String json = convertToJson(data);

        String description = getDescription(cd);

        // TODO: Hent scope fra application.yaml
        String scope = "eudiw:eidas2sandkasse:dynamicvc";

        return new CredentialConfiguration(
                cd.getCredentialConfigurationId(),
                scope,
                personIdentifier,
                description,
                json
        );
    }

    private static String getDescription(CredentialDefinition cd) {
        return cd
                .getCredentialMetadata()
                .display()
                .stream()
                .findFirst()
                .map(Display::name)
                .orElse("Navn ikke funnet");
    }

    private IssuanceDefinition convertFromCredentialDefinitionToIssuanceDefinition(CredentialDefinition cd, String personId) {
        if (cd == null) {
            throw new IssuerUiException("CredentialDefinition is null");

        }
        return new IssuanceDefinition(cd.getCredentialConfigurationId(), personId, convertFromExampleDataToIssuanceClaim(cd));
    }

    private List<IssuanceClaim> convertFromExampleDataToIssuanceClaim(CredentialDefinition cd) {
        ArrayList<IssuanceClaim> issuanceClaims = new ArrayList<>();

        for (ExampleCredentialData exampleData : cd.getExampleCredentialData()) {
            issuanceClaims.add(new IssuanceClaim(exampleData.name(), exampleData.value()));
        }

        return issuanceClaims;
    }

    private String convertToJson(IssuanceDefinition issuanceDefinition) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(issuanceDefinition);
        } catch (JsonProcessingException e) {
            throw new IssuerUiException("Failed to parse issuanceDefinition: %s".formatted(issuanceDefinition.credentialConfigurationId()), e);
        }
    }
}
