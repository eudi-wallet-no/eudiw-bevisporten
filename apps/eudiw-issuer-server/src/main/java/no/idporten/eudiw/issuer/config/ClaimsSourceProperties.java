package no.idporten.eudiw.issuer.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class ClaimsSourceProperties {

    @NotEmpty
    private Set<String> credentialTypes;
    @NotNull
    private String className;

}
