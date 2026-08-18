package no.idporten.eudiw.issuer.issuance.status;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;

/**
 * Tracking status of issuance in pre-authorized code flow.
 *
 * @param issuanceTransactionId
 * @param credentialConfigurationId credential configuration id
 * @param status                    credential issuance status
 */
public record CredentialIssuanceStatus(
        @JsonProperty("issuance_transaction_id")
        IssuanceTransactionId issuanceTransactionId,
        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId,
        @JsonProperty("status")
        String status
) {
}
