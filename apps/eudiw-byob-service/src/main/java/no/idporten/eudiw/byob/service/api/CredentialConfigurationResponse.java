package no.idporten.eudiw.byob.service.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CredentialConfigurationResponse(@JsonProperty("credential_configurations") List<Object> credentialConfigurations) {
}
