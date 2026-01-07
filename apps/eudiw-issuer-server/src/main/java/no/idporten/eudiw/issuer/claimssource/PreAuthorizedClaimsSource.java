package no.idporten.eudiw.issuer.claimssource;

import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Collections;
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
     * @param pushedClaims pushed credential data
     */
    default void preAuthorize(PreAuthorizedIssuanceContext issuanceContext, Map<String, String> pushedClaims) {
        final CredentialData credentialData;
        final DocumentMetadata documentMetadata = getDocumentMetadata(new CredentialMetadataContext(issuanceContext.credentialConfigurationId(), null, null));
        if (!CollectionUtils.isEmpty(pushedClaims)) {
            credentialData = validate(documentMetadata, push(issuanceContext, new CredentialData(Collections.unmodifiableMap(pushedClaims), issuanceContext.credentialConfigurationId())));
        } else {
            credentialData = validate(documentMetadata, pull(issuanceContext));
        }
        store(issuanceContext.issuanceTransactionId(), credentialData.claims(), issuanceContext.authorizationLifetime());
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
    CredentialData validate(DocumentMetadata credentialMetadata, CredentialData credentialData);

    /**
     * Store claims data in cache for a given lifetime.
     */
    IssuanceTransactionId store(IssuanceTransactionId issuanceTransactionId, Map<String, Object> claims, Duration lifetime);

}
