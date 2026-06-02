package no.idporten.eudiw.issuer.openid4vci;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.CredentialCreateService;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.metrics.MetricService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenCredentialValidationContext;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
import no.idporten.eudiw.issuer.openid4vci.proofs.ProofService;
import no.idporten.eudiw.issuer.openid4vci.protocol.*;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.List;


@RequiredArgsConstructor
@Service
public class CredentialIssuerService {

    private final ClaimsSourceService claimsSourceService;
    private final AccessTokenValidationService accessTokenValidationService;
    private final ProofService proofService;
    private final CredentialCreateService credentialCreateService;
    private final CredentialIssuanceStatusService credentialIssuanceStatusService;
    private final AuditService auditService;
    private final MetricService metricService;

    @SneakyThrows
    public CredentialResponse issueCredentials(CredentialIssuerTenant tenant, CredentialRequest credentialRequest, JWT accessToken) {
        ExtendedCredentialConfiguration credentialConfiguration = tenant.findCredentialConfiguration(credentialRequest.getCredentialConfigurationId());
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, AccessTokenCredentialValidationContext.forAuthorization(credentialConfiguration));
        ClaimsSource claimsSource = claimsSourceService.findClaimsSource(credentialConfiguration.getCredentialIssuerContext().getCredentialDataSourceUri());
        List<JWK> bindingKeys = proofService.validateProofs(tenant, credentialRequest.getProofs());
        CredentialIssueContext credentialIssueContext = new CredentialIssueContext(accessToken, tenant, credentialConfiguration);
        List<Claim> claims = claimsSource.issueClaims(credentialIssueContext);
        List<Credential> credentials = credentialCreateService.createCredentials(credentialIssueContext, bindingKeys, claims);
        IssuanceTransactionId issuanceTransactionId = getIssuanceTransactionId(accessToken);
        NotificationId notificationId = credentialIssuanceStatusService.credentialIssued(credentialRequest.getCredentialConfigurationId(), issuanceTransactionId);

        auditService.logIssueCredentials(credentialConfiguration.getCredentialIssuerContext().getAuthorizationServer(), credentialRequest.getCredentialConfigurationId(), issuanceTransactionId, credentialConfiguration.getFormat(), credentials.size(), notificationId, accessToken);
        metricService.countIssued(credentialRequest.getCredentialConfigurationId());
        return CredentialResponse.builder()
                .credentials(credentials)
                .notificationId(notificationId)
                .build();
    }

    private IssuanceTransactionId getIssuanceTransactionId(JWT accessToken) {
        try {
            if (accessToken.getJWTClaimsSet().getStringClaim("tx_id") != null) {
                return new IssuanceTransactionId(accessToken.getJWTClaimsSet().getStringClaim("tx_id"));
            }
        } catch (ParseException e) {
            throw new InvalidCredentialRequest("Invalid token");
        }
        return null;
    }

}
