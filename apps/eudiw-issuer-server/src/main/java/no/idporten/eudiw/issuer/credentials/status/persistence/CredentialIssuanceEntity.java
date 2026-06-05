package no.idporten.eudiw.issuer.credentials.status.persistence;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CredentialIssuanceEntity {

    private Long id;
    private String issuanceTransactionId;
    private String credentialConfigurationId;
    private String credentialIssuerTenant;
    private long createdMs;
    private long updatedMs;

    public CredentialIssuanceEntity(
            Long id,
            String issuanceTransactionId,
            String credentialConfigurationId,
            String credentialIssuerTenant,
            long createdMs,
            long updatedMs
    ) {
        this.id = id;
        this.issuanceTransactionId = issuanceTransactionId;
        this.credentialConfigurationId = credentialConfigurationId;
        this.credentialIssuerTenant = credentialIssuerTenant;
        this.createdMs = createdMs;
        this.updatedMs = updatedMs;
    }

    public CredentialIssuanceEntity(String issuanceTransactionId, String credentialConfigurationId, String credentialIssuerTenant, long createdMs, long updatedMs) {
        this(null, issuanceTransactionId, credentialConfigurationId, credentialIssuerTenant, createdMs, updatedMs);
    }
}
