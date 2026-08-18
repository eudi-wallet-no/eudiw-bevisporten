package no.idporten.eudiw.issuer.credentials.status.persistence;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusListEntryEntity {

    private Long id;
    private Long credentialIssuanceTransactionId;
    private String uri;
    private int idx;

    public StatusListEntryEntity(Long id, Long credentialIssuanceTransactionId, String uri, int idx) {
        this.id = id;
        this.credentialIssuanceTransactionId = credentialIssuanceTransactionId;
        this.uri = uri;
        this.idx = idx;
    }

    public StatusListEntryEntity(Long credentialIssuanceTransactionId, String uri, int idx) {
        this(null, credentialIssuanceTransactionId, uri, idx);
    }
}
