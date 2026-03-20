package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Map;

/**
 * A pre-authorized claims source ensures that data is present and stored before a credential offer is created.
 * The claims source will either support a push mode (receive the data from the client application), or a pull mode
 * (it can retrieve the data itself).  Data is validated and stored by the claims source.
 */
public sealed interface PreAuthorizedClaimsSource extends ClaimsSource permits AbstractPreAuthorizedClaimsSource {

    /**
     * Pre-authorize credential issuance by validating and storing claims data.  Data can be provided by calling
     * system or fetched by the claims source if the pull() method is implemented.
     *
     * @param issuanceContext the issuance context
     * @param credentialData pushed credential data
     */
    default void preAuthorize(PreAuthorizedIssuanceContext issuanceContext, CredentialData credentialData) {
        final ExtendedCredentialMetadata extendedCredentialMetadata = issuanceContext.credentialConfiguration().getExtendedCredentialMetadata();
        if (credentialData != null && !CollectionUtils.isEmpty(credentialData.claims())) {
            credentialData = validate(extendedCredentialMetadata, push(issuanceContext, credentialData));
        } else {
            credentialData = validate(extendedCredentialMetadata, pull(issuanceContext));
        }
        store(issuanceContext, credentialData.claims(), issuanceContext.authorizationLifetime());
    }

    /**
     * Pull claims data from authoritative source.  Disabled by default.
     */
    default CredentialData pull(PreAuthorizedIssuanceContext preAuthorizedIssuanceContext) {
        throw new IssuerServerException("invalid_request", "Credential configuration does not support pull of data", HttpStatus.BAD_REQUEST);
    }

    /**
     * Receive pushed data from the authoritative source.  Disabled by default.
     */
    default CredentialData push(PreAuthorizedIssuanceContext preAuthorizedIssuanceContext, CredentialData credentialData) {
        throw new IssuerServerException("invalid_request", "Credential configuration does not support push of data", HttpStatus.BAD_REQUEST);
    }

    /**
     * Validate that claims data is valid according to credential metadata.
     */
    CredentialData validate(ExtendedCredentialMetadata credentialMetadata, CredentialData credentialData);

    /**
     * Store claims data in cache for a given lifetime.
     */
    IssuanceTransactionId store(PreAuthorizedIssuanceContext preAuthorizedIssuanceContext, Map<String, Object> claims, Duration lifetime);

}
