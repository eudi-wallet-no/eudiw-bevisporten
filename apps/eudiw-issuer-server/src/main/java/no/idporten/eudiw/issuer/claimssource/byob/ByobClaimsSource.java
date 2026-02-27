package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import org.springframework.stereotype.Service;

import static no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource.BYOB;

@Service
public class ByobClaimsSource extends AbstractPreAuthorizedClaimsSource {

    @Override
    public CredentialData push(PreAuthorizedIssuanceContext issuanceContext, CredentialData credentialData) {
        return credentialData;
    }

    @Override
    public String getAuthorativeSourceName() {
        return BYOB.name();
    }

}
