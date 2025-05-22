package no.idporten.eudiw.issuer.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @NotNull
    private String authorizationServer;

}
