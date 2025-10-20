package no.idporten.eudiw.issuer.ui.demo.issuer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CredentialOffer(@JsonProperty("credential_issuer") String credentialIssuer, @JsonProperty("credential_configuration_ids") List<String> credentialConfigurationIds, @JsonProperty("grants") Grants grants) {

}
