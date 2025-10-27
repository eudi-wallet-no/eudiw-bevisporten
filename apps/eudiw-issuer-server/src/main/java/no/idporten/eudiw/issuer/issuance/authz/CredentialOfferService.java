package no.idporten.eudiw.issuer.issuance.authz;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.openid4vci.protocol.AuthorizedCodeGrant;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialOffer;
import no.idporten.eudiw.issuer.openid4vci.protocol.Grants;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class CredentialOfferService {

    public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final AuditService auditService;

    public CredentialOffer createCredentialOffer(String credentialConfigurationId) {
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(credentialConfigurationId);
        if (!GRANT_TYPE_AUTHORIZATION_CODE.equals(credentialConfigurationProperties.getGrantType())) {
            throw new IssuerServerException("invalid_request", "Credential configuration cannot be used with the authorization code flow", HttpStatus.BAD_REQUEST);
        }
        auditService.logCreateCredentialOffer(credentialIssuerServerProperties.getCredentialIssuer().toString(), credentialConfigurationId);
        return CredentialOffer.builder()
                .credentialIssuer(credentialIssuerServerProperties.getCredentialIssuer().toString())
                .credentialConfigurationId(credentialConfigurationProperties.getIdentifier())
                .grants(Grants.builder().authorizedCodeGrant(AuthorizedCodeGrant.builder().build()).build())
                .build();
    }

}
