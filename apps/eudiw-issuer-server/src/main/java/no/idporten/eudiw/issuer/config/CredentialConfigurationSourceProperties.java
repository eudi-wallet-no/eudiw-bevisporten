package no.idporten.eudiw.issuer.config;

import jakarta.validation.Valid;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for credential configurations.
 *
 * @param api api connection properties for reading credential definitions from an external source
 * @param local list of paths to credential definitions files
 */
@Validated
public record CredentialConfigurationSourceProperties(
        @Valid APIConnectionProperties api,
        @DefaultValue() LocalResourceProperties local) {

}
