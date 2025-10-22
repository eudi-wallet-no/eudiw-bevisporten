package no.idporten.eudiw.issuer.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClaimsSourceProperties {

    @NotNull
    private String credentialType;
    @NotNull
    private String className;

}
