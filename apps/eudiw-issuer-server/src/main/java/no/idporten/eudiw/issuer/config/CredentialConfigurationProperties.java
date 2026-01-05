package no.idporten.eudiw.issuer.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.idporten.eudiw.issuer.openid4vci.CredentialFormat;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CredentialConfigurationProperties {

    /**
     * Credential identifier used in metadata, requests, responses
     */
    @NotEmpty
    private String identifier;

    /**
     * Document type used in metadata.
     */
    @NotNull
    private String credentialType;

    /**
     * Credential format
     */
    @NotNull
    private CredentialFormat format;

    /**
     * For how many days the credential is technically valid
     */
    @Min(1)
    @Builder.Default
    private int validityDays = 365;

    /**
     * Scope required in access_token at the credentials endpoint
     */
    @NotNull
    private String scope;

    /**
     * Grant type supported to issue this credential.
     */
    @Pattern(regexp = "authorization_code|urn:ietf:params:oauth:grant-type:pre-authorized_code")
    private String grantType;

    /**
     * Issuer of access_token at the credentials endpoint
     */
    @NotNull
    private String authorizationServer;

    /**
     * Issuer of access_token at the start issuance endpoint in the pre-authorized flow
     */
    private String preAuthorizationServer;

    /**
     * Pre-authorization and credential offer lifetime in the pre-authorized flow
     */
    @NotNull
    private Duration preAuthorizationLifetime = Duration.ofMinutes(10);

    /**
     * Pre-authorization code flow requires tx code or not.
     */
    private boolean requireTxCode = true;

    /**
     * Name of keystore for provider signing certificate
     */
    @NotNull
    private String keyStoreName;

    /**
     * Hook for dynamic credential configurations?
     */
    private boolean dynamic = false;

}
