package no.idporten.eudiw.issuer.claimssource;

import java.util.List;

public interface PreAuthorizedClaimsSource extends ClaimsSource {

    /**
     * Store claims
     *
     * @param txId
     * @param claims
     * @return tx_id
     */
    String store(String txId, List<Claim> claims);

}
