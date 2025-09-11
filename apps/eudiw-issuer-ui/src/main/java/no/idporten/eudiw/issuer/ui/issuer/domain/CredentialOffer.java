package no.idporten.eudiw.issuer.ui.issuer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.idporten.eudiw.issuer.ui.exception.IssuerUiException;

import java.util.List;

public record CredentialOffer(@JsonProperty("credential_issuer") String credentialIssuer, @JsonProperty("credential_configuration_ids") List<String> credentialConfigurationIds, @JsonProperty("grants") Grants grants) {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public String toJsonString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IssuerUiException("Failed to convert credentialOffer to Json string", e);
        }
    }
    public String toPrettyJsonString() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IssuerUiException("Failed to convert credentialOffer to pretty Json string", e);
        }
    }
}
