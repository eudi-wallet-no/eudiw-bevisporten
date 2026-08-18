package no.idporten.eudiw.issuer.credentials;

import com.nimbusds.jose.jwk.JWK;
import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.credentials.formats.MDocService;
import no.idporten.eudiw.issuer.credentials.formats.SDJWTService;
import no.idporten.eudiw.issuer.credentials.status.CredentialStatus;
import no.idporten.eudiw.issuer.credentials.status.StatusIssuerService;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CredentialCreateService {

    private final MDocService mDocService;
    private final SDJWTService sdjwtService;
    private final StatusIssuerService statusIssuerService;

    @Autowired
    public CredentialCreateService(MDocService mDocService, SDJWTService sdjwtService, StatusIssuerService statusIssuerService) {
        this.mDocService = mDocService;
        this.sdjwtService = sdjwtService;
        this.statusIssuerService = statusIssuerService;
    }

    /**
     * Creates credentials in the format configured on credential configuration.
     */
    public List<Credential> createCredentials(CredentialIssueContext context, List<JWK> bindingKeys, List<Claim> claims) {
        if (bindingKeys == null) {
            // TODO this is most likely invalid - test with android and ios updated to OpenID4VCI 1!
            return List.of(createCredential(context, null, claims, allocateStatus(context)));
        }
        List<CredentialStatus> credentialStatuses = allocateStatus(context, bindingKeys.size());
        List<Credential> credentials = new ArrayList<>();
        for (int i = 0; i < bindingKeys.size(); i++) {
            credentials.add(createCredential(context, bindingKeys.get(i), claims, credentialStatuses != null ? credentialStatuses.get(i) : null));
        }
        return credentials;
    }

    private Credential createCredential(CredentialIssueContext context, JWK bindingKey, List<Claim> claims, CredentialStatus status) {
        return switch (context.credentialConfiguration().getFormat()) {
            case MSO_MDOC -> mDocService.issueCredential(context, bindingKey, claims, status);
            case SD_JWT_VC -> sdjwtService.issueCredential(context, bindingKey, claims, status);
            case null -> throw new IssuerServerException(ErrorCode.SERVER_ERROR, "Missing credential format.");
        };
    }

    private CredentialStatus allocateStatus(CredentialIssueContext context) {
        return statusIssuerService.isEnabled(context) ? statusIssuerService.allocateStatus(context, 1).getFirst() : null;
    }

    private List<CredentialStatus> allocateStatus(CredentialIssueContext context, int instances) {
        return statusIssuerService.isEnabled(context) ? statusIssuerService.allocateStatus(context, instances) : null;
    }

}
