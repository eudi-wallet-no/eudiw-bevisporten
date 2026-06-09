package no.idporten.eudiw.issuer.credentials.status.persistence;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CredentialIssuanceTransactionEntity {

    private Long id;
    private String issuanceTransactionId;
    private String credentialConfigurationId;
    private String credentialIssuerTenant;
    private long createdMs;
    private long updatedMs;
    private String status;
    private String notificationId;

    public CredentialIssuanceTransactionEntity(
            Long id,
            String issuanceTransactionId,
            String credentialConfigurationId,
            String credentialIssuerTenant,
            long createdMs,
            long updatedMs,
            String status,
            String notificationId
    ) {
        this.id = id;
        this.issuanceTransactionId = issuanceTransactionId;
        this.credentialConfigurationId = credentialConfigurationId;
        this.credentialIssuerTenant = credentialIssuerTenant;
        this.createdMs = createdMs;
        this.updatedMs = updatedMs;
        this.status = status;
        this.notificationId = notificationId;
    }

    public CredentialIssuanceTransactionEntity(String issuanceTransactionId, String credentialConfigurationId, String credentialIssuerTenant, long createdMs, long updatedMs) {
        this(null, issuanceTransactionId, credentialConfigurationId, credentialIssuerTenant, createdMs, updatedMs, null, null);
    }
}
