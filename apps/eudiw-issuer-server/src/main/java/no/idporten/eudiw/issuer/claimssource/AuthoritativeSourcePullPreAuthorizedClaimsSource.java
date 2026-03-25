package no.idporten.eudiw.issuer.claimssource;

import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSourceService;
import org.springframework.stereotype.Service;

import static no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource.BYOB;

/**
 * Generic claims source for pre-authorized issuance where the credential data is pulled from authoritative source
 * standard API.
 */
@Service
public class AuthoritativeSourcePullPreAuthorizedClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final AuthoritativeSourceService authoritativeSourceService;

    public AuthoritativeSourcePullPreAuthorizedClaimsSource(AuthoritativeSourceService authoritativeSourceService) {
        this.authoritativeSourceService = authoritativeSourceService;
    }

    @SneakyThrows
    @Override
    public CredentialData pull(PreAuthorizedIssuanceContext issuanceContext) {
        final String personIdentifier = issuanceContext.accessToken().getJWTClaimsSet().getStringClaim("pid");
        final String credentialType = issuanceContext.credentialConfiguration().getCredentialType();
        final String source = issuanceContext.credentialConfiguration().getCredentialIssuerContext().getCredentialDataSourceUri().getAuthority();
        return authoritativeSourceService.retrieveCredentialData(source, credentialType, personIdentifier);
    }

    @Override
    public String getAuthorativeSourceName() {
        // TODO fjerne dette fra utsteder og exceptions, gir ikke så stor mening når alle er flyttet til ny connector - egen sak!
        return BYOB.name();
    }

}
