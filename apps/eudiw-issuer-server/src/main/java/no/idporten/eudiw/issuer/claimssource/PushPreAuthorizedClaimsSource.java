package no.idporten.eudiw.issuer.claimssource;

import org.springframework.stereotype.Service;

import static no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource.BYOB;

/**
 * Generic claims source for pre-authorized issuance where the credential data is pushed to the claims source.
 */
@Service
public class PushPreAuthorizedClaimsSource extends AbstractPreAuthorizedClaimsSource {

    @Override
    public CredentialData push(PreAuthorizedIssuanceContext issuanceContext, CredentialData credentialData) {
        return credentialData;
    }

    @Override
    public String getAuthorativeSourceName() {
        return BYOB.name();
    }

}
