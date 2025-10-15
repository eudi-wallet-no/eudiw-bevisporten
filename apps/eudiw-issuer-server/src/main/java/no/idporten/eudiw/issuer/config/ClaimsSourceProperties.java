package no.idporten.eudiw.issuer.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.net.URI;

@Data
public class ClaimsSourceProperties {

    @NotNull
    private String credentialType;
    private URI resourceServer;
    @Min(1)
    private int connectTimeoutMillis = 3000;
    @Min(1)
    private int readTimeoutMillis = 5000;
    @NotNull
    private String className;

}
