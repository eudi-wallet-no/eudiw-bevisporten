package no.idporten.eudiw.issuer.config;

import org.springframework.validation.annotation.Validated;

@Validated
public record CredentialConfigurationSourceProperties(APIConnectionProperties api) {

}
