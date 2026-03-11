package no.idporten.eudiw.connector.authoritativsources.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record CredentialDataResponse(@JsonProperty("credential_data") CredentialData credentialData) { }
