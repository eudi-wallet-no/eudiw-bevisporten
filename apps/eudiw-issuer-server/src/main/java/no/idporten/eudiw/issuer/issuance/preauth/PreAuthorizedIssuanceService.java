package no.idporten.eudiw.issuer.issuance.preauth;

import com.nimbusds.jwt.JWT;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceMetadata;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatus;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.preauth.PreAuthorizationService;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialOffer;
import no.idporten.eudiw.issuer.openid4vci.protocol.Grants;
import no.idporten.eudiw.issuer.openid4vci.protocol.PreAuthorizedCodeGrant;
import no.idporten.eudiw.issuer.openid4vci.protocol.TxCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class PreAuthorizedIssuanceService {

    public static final String GRANT_TYPE_PRE_AUTHORIZED_CODE = "urn:ietf:params:oauth:grant-type:pre-authorized_code";
    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final ClaimsSourceService claimsSourceService;
    private final AccessTokenValidationService accessTokenValidationService;
    private final PreAuthorizationService preAuthorizationService;
    private final CredentialIssuanceStatusService credentialIssuanceStatusService;
    private final AuditService auditService;

    public PreAuthorizedIssuanceResponse startIssuerTransaction(PreAuthorizedIssuanceRequest preAuthorizedIssuanceRequest, JWT accessToken) {
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(preAuthorizedIssuanceRequest.getCredentialConfigurationId());
        if (!GRANT_TYPE_PRE_AUTHORIZED_CODE.equals(credentialConfigurationProperties.getGrantType())) {
            throw new IssuerServerException("invalid_request", "Credential configuration can only be used with the pre-authorized code flow", HttpStatus.BAD_REQUEST);
        }
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, credentialConfigurationProperties.getPreAuthorizationServer(), credentialConfigurationProperties.getScope());
        PreAuthorizedClaimsSource claimsSource = (PreAuthorizedClaimsSource) claimsSourceService.findClaimsSource(credentialConfigurationProperties.getCredentialType());
        ClaimsSourceMetadata metadata = claimsSourceService.getMetadata(claimsSource);
        final IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        final String preAuthorizedCode = preAuthorizationService.preAuthorize(issuanceTransactionId, preAuthorizedIssuanceRequest);
        claimsSource.preAuthorize(new PreAuthorizedIssuanceContext(issuanceTransactionId, preAuthorizedIssuanceRequest.getCredentialConfigurationId(), accessToken, credentialConfigurationProperties.getPreAuthorizationLifetime()), preAuthorizedIssuanceRequest.getClaimsMap());
        CredentialOffer credentialOffer = CredentialOffer.builder()
                .credentialIssuer(credentialIssuerServerProperties.getCredentialIssuer().toString())
                .credentialConfigurationId(credentialConfigurationProperties.getIdentifier())
                .grants(Grants.builder()
                        .preAuthorizedCodeGrant(PreAuthorizedCodeGrant.builder().preAuthorizedCode(preAuthorizedCode)
                                .txCode(credentialConfigurationProperties.isRequireTxCode() ?
                                        TxCode.builder()
                                                .inputMode("numeric")
                                                .length(4)
                                                .description("Enter code from SMS to issue %s".formatted(metadata.getDisplays().getFirst().getName()))
                                                .build()
                                        : null)
                                .build())
                        .build())
                .build();
        CredentialIssuanceStatus issuanceStatus = credentialIssuanceStatusService.offerIssued(issuanceTransactionId, preAuthorizedIssuanceRequest.getCredentialConfigurationId());
        auditService.logStartCredentialIssuanceTransaction(credentialOffer.getCredentialIssuer(), preAuthorizedIssuanceRequest.getCredentialConfigurationId(), issuanceTransactionId, accessToken);
        return PreAuthorizedIssuanceResponse.builder()
                .credentialOffer(credentialOffer)
                .issuanceTransactionId(issuanceTransactionId)
                .build();
    }

}
