package no.idporten.eudiw.issuer.claimssource;

import java.util.Map;

public interface PreAuthorizedClaimsSource extends ClaimsSource {

    /**
     * Store claims
     *
     * @param txId
     * @param claims
     * @return tx_id
     */
    String store(String txId, Map<String, String> claims);

}
