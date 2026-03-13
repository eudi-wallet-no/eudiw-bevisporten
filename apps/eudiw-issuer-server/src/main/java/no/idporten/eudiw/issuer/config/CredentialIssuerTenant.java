package no.idporten.eudiw.issuer.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialIssuerTenant {

    /**
     * The tenant identifier for this credential issuer tenant.
     */
    private String id;

    /**
     * The credential issuer uri for this credential issuer tenant.
     */
    private URI credentialIssuer;

    /**
     * The display names used in metadata for this credential issuer tenant.
     */
    private Map<String, String> displayNames = Map.of("no", "Digitaliseringsdirektoratet");

    /**
     * The credential configuration sources containing the credential configurations supported by this credential issuer tenant.
     */
    private List<CredentialConfigurationSource> credentialConfigurationSources = new ArrayList<>();

    /**
     * The properties for the credential configuration sources to load at startup.
     */
    private List<CredentialConfigurationSourceProperties> credentialConfigurationSourcesProperties = new ArrayList<>();

    /**
     * Find credential identifiers supported by this credential issuer tenant.
     */
    public ExtendedCredentialConfiguration findCredentialConfiguration(String credentialIdentifier) {
        return credentialConfigurationSources.stream()
                .map(ccs -> ccs.findConfiguration(credentialIdentifier))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IssuerServerException(
                        "unknown_credential_identifier",
                        "Unknown credential identifier.",
                        "Credential issuer %s does not support credential configuration %s".formatted(credentialIssuer, credentialIdentifier),
                        HttpStatus.BAD_REQUEST));
    }

    public boolean isRootCredentialIssuer() {
        return CredentialIssuerTenantService.ROOT_TENANT_ID.equals(id);
    }

}
