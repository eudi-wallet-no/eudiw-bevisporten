package no.idporten.eudiw.bevisgenerator.integration.issuerserver.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CredentialOffer(@JsonProperty("credential_issuer") String credentialIssuer, @JsonProperty("credential_configuration_ids") List<String> credentialConfigurationIds, @JsonProperty("grants") Grants grants) {

}
