package no.idporten.eudiw.issuer.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
public class CredentialConfigurationProperties {

    @NotEmpty
    private String identifier;
    @NotNull
    private String doctype;
    @NotNull
    private String format;
    @NotNull
    private String scope;

}
