package no.idporten.eudiw.issuer.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@JsonIgnoreProperties(ignoreUnknown = true)
@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CredentialIssuerContext {

    /**
     * Grant type supported to issue this credential.
     */
    @JsonProperty("grant_type")
    @Pattern(regexp = "authorization_code|urn:ietf:params:oauth:grant-type:pre-authorized_code")
    private String grantType;
    /**
     * Issuer of access_token at the credentials endpoint
     */
    @NotNull
    @JsonProperty("authorization_server")
    private String authorizationServer;
    /**
     * Issuer of access_token at the start issuance endpoint in the pre-authorized flow
     */
    @JsonProperty("pre_authorization_server")
    private String preAuthorizationServer;
    /**
     * Pre-authorization and credential offer lifetime in the pre-authorized flow
     */
    @JsonProperty("pre_authorization_lifetime")
    @NotNull
    private Duration preAuthorizationLifetime = Duration.ofMinutes(10);
    /**
     * For how many days the credential is technically valid
     */
    @JsonProperty("validity_days")
    @Min(1)
    private int validityDays = 365;

    /**
     * Pre-authorization code flow requires tx code or not.
     */
    @JsonProperty("require_tx_code")
    private boolean requireTxCode = true;

    @JsonProperty("credential_datasource_uri")
    @NotNull
    private URI credentialDataSourceUri;

    /**
     * Name of keystore for provider signing certificate
     */
    @JsonProperty("credential_signing_keystore")
    @NotNull
    private String credentialSigningKeystore;

}