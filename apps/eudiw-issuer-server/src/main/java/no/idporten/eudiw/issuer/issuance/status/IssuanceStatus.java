package no.idporten.eudiw.issuer.issuance.status;

import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;

public record IssuanceStatus(
        IssuanceTransactionId issuanceTransactionId,
        String credentialConfigurationId,
        String status) {
}
