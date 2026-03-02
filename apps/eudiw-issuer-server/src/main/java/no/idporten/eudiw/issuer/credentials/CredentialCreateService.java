package no.idporten.eudiw.issuer.credentials;

import com.nimbusds.jose.jwk.JWK;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.formats.MDocService;
import no.idporten.eudiw.issuer.credentials.formats.SDJWTService;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public List<Credential> createCredentials(List<JWK> bindingKeys, ExtendedCredentialConfiguration credentialConfiguration, List<Claim> claims) {
        if (bindingKeys == null) {
            // TODO this is most likely invalid - test with android and ios updated to OpenID4VCI 1!
            return List.of(createCredential(null, credentialConfiguration, claims));
        }
        return bindingKeys.stream().map(bindingKey -> createCredential(bindingKey, credentialConfiguration, claims)).toList();
    }

    private Credential createCredential(JWK bindingKey, ExtendedCredentialConfiguration credentialConfiguration, List<Claim> claims) {
        return switch (credentialConfiguration.getFormat()) {
            case MSO_MDOC -> mDocService.issueCredential(bindingKey, credentialConfiguration, claims);
            case SD_JWT_VC -> sdjwtService.issueCredential(bindingKey, credentialConfiguration, claims);
            case null -> throw new IssuerServerException("server_error", "Missing credential format.", HttpStatus.INTERNAL_SERVER_ERROR);
        };
    }

}
