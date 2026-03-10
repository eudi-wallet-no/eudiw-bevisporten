package no.idporten.eudiw.issuer.issuance.preauth;

import com.nimbusds.jwt.JWT;
import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialMetadata;
import no.idporten.eudiw.issuer.issuance.preauth.integration.PreAuthorizationIntegration;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatus;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenCredentialValidationContext;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
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
    private final PreAuthorizationIntegration preAuthorizationIntegration;
    private final CredentialIssuanceStatusService credentialIssuanceStatusService;
    private final AuditService auditService;

    public PreAuthorizedIssuanceResponse startIssuerTransaction(CredentialIssuerTenant credentialIssuerTenant, PreAuthorizedIssuanceRequest preAuthorizedIssuanceRequest, JWT accessToken) {
        ExtendedCredentialConfiguration credentialConfiguration = credentialIssuerTenant.findCredentialConfiguration(preAuthorizedIssuanceRequest.getCredentialConfigurationId());
        if (! credentialIssuerTenant.getCredentialIssuer().toString().equals(preAuthorizedIssuanceRequest.getCredentialIssuer())) {
            throw new IssuerServerException(
                    "invalid_request",
                    "Invalid credential issuer in request.",
                    "Expected issuer %s but client used %s to pre-authorize for %s".formatted(credentialIssuerTenant.getCredentialIssuer(), preAuthorizedIssuanceRequest.getCredentialIssuer(), preAuthorizedIssuanceRequest.getCredentialConfigurationId()),
                    HttpStatus.BAD_REQUEST);
        }
        if (!GRANT_TYPE_PRE_AUTHORIZED_CODE.equals(credentialConfiguration.getCredentialIssuerContext().getGrantType())) {
            throw new IssuerServerException("invalid_request", "Credential configuration can only be used with the pre-authorized code flow", HttpStatus.BAD_REQUEST);
        }
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, AccessTokenCredentialValidationContext.forPreAuthorization(credentialConfiguration.getCredentialIssuerContext().getPreAuthorizationServer(), credentialConfiguration.getScope()));
        PreAuthorizedClaimsSource claimsSource = (PreAuthorizedClaimsSource) claimsSourceService.findClaimsSource(credentialConfiguration.getCredentialIssuerContext().getCredentialDataSourceUri());
        ExtendedCredentialMetadata metadata = credentialConfiguration.getExtendedCredentialMetadata();
        final IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        final String preAuthorizedCode = preAuthorizationIntegration.preAuthorize(credentialIssuerTenant, issuanceTransactionId, preAuthorizedIssuanceRequest);
        claimsSource.preAuthorize(new PreAuthorizedIssuanceContext(issuanceTransactionId, credentialConfiguration.getExtendedCredentialMetadata(), preAuthorizedIssuanceRequest.getCredentialConfigurationId(), accessToken, credentialConfiguration.getCredentialIssuerContext().getPreAuthorizationLifetime()), preAuthorizedIssuanceRequest.getCredentialData());
        CredentialOffer credentialOffer = CredentialOffer.builder()
                .credentialIssuer(credentialIssuerTenant.getCredentialIssuer().toString())
                .credentialConfigurationId(credentialConfiguration.getCredentialConfigurationId())
                .grants(Grants.builder()
                        .preAuthorizedCodeGrant(PreAuthorizedCodeGrant.builder().preAuthorizedCode(preAuthorizedCode)
                                .txCode(credentialConfiguration.getCredentialIssuerContext().isRequireTxCode() ?
                                        TxCode.builder()
                                                .inputMode("numeric")
                                                .length(4)
                                                .description("Enter code from SMS to issue %s".formatted(metadata.display().getFirst().getName()))
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
