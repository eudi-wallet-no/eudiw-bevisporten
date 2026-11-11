package no.idporten.eudiw.issuer.openid4vci.proofs;

import java.util.List;

/**
 * Binding keys from validating the proofs in a credential request, grouped by proof.
 *
 * @param keys attested keys, one entry for each proof in the request
 */
public record BindingKeys(List<AttestedKeys> keys) {

    public BindingKeys {
        keys = List.copyOf(keys);
    }

}
