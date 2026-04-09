package no.idporten.eudiw.issuer.credentials;

import com.nimbusds.jose.jwk.JWK;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.exception.ErrorCode;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.formats.MDocService;
import no.idporten.eudiw.issuer.credentials.formats.SDJWTService;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CredentialCreateService {

    private final MDocService mDocService;
    private final SDJWTService sdjwtService;

    @Autowired
    public CredentialCreateService(MDocService mDocService, SDJWTService sdjwtService) {
        this.mDocService = mDocService;
        this.sdjwtService = sdjwtService;
    }

    /**
     * Creates credentials in the format configured on credential configuration.
     */
    public List<Credential> createCredentials(CredentialIssuerTenant tenant, List<JWK> bindingKeys, ExtendedCredentialConfiguration credentialConfiguration, List<Claim> claims) {
        if (bindingKeys == null) {
            // TODO this is most likely invalid - test with android and ios updated to OpenID4VCI 1!
            return List.of(createCredential(tenant, null, credentialConfiguration, claims));
        }
        return bindingKeys.stream().map(bindingKey -> createCredential(tenant, bindingKey, credentialConfiguration, claims)).toList();
    }

    private Credential createCredential(CredentialIssuerTenant tenant, JWK bindingKey, ExtendedCredentialConfiguration credentialConfiguration, List<Claim> claims) {
        return switch (credentialConfiguration.getFormat()) {
            case MSO_MDOC -> mDocService.issueCredential(tenant, bindingKey, credentialConfiguration, claims);
            case SD_JWT_VC -> sdjwtService.issueCredential(tenant, bindingKey, credentialConfiguration, claims);
            case null -> throw new IssuerServerException(ErrorCode.SERVER_ERROR, "Missing credential format.");
        };
    }

}
