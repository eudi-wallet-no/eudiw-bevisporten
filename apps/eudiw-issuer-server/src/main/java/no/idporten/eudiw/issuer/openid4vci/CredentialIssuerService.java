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
import no.idporten.eudiw.issuer.credentials.status.persistence.CredentialIssuanceTransactionDao;
import no.idporten.eudiw.issuer.credentials.status.persistence.CredentialIssuanceTransactionEntity;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.issuance.CredentialIssuanceType;
import no.idporten.eudiw.issuer.issuance.authz.SubjectCredentialTransactionDao;
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
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.List;


@RequiredArgsConstructor
@Service
public class CredentialIssuerService {

    private final ClaimsSourceService claimsSourceService;
    private final AccessTokenValidationService accessTokenValidationService;
    private final ProofService proofService;
    private final CredentialCreateService credentialCreateService;
    private final CredentialIssuanceTransactionDao credentialIssuanceTransactionDao;
    private final SubjectCredentialTransactionDao subjectCredentialTransactionDao;
    private final CredentialIssuanceStatusService credentialIssuanceStatusService;
    private final AuditService auditService;
    private final MetricService metricService;

    @Transactional
    @SneakyThrows
    public CredentialResponse issueCredentials(CredentialIssuerTenant tenant, CredentialRequest credentialRequest, JWT accessToken) {
        ExtendedCredentialConfiguration credentialConfiguration = tenant.findCredentialConfiguration(credentialRequest.getCredentialConfigurationId());
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, AccessTokenCredentialValidationContext.forAuthorization(credentialConfiguration));
        ClaimsSource claimsSource = claimsSourceService.findClaimsSource(credentialConfiguration.getCredentialIssuerContext().getCredentialDataSourceUri());
        List<JWK> bindingKeys = proofService.validateProofs(tenant, credentialRequest.getProofs());

        CredentialIssuanceType issuanceType = CredentialIssuanceType.fromAccessToken(accessToken);
        IssuanceTransactionId issuanceTransactionId = resolveIssuanceTransactionId(accessToken, issuanceType, credentialRequest, tenant);

        CredentialIssueContext credentialIssueContext = new CredentialIssueContext(accessToken, tenant, credentialConfiguration, issuanceTransactionId, issuanceType);
        List<Claim> claims = claimsSource.issueClaims(credentialIssueContext);
        List<Credential> credentials = credentialCreateService.createCredentials(credentialIssueContext, bindingKeys, claims);
        createSubjectCredentialTransaction(credentialIssueContext, issuanceTransactionId);

        NotificationId notificationId = switch (issuanceType) {
            case PRE_AUTHORIZED_CODE -> credentialIssuanceStatusService.credentialIssued(credentialRequest.getCredentialConfigurationId(), issuanceTransactionId);
            case AUTHORIZATION_CODE -> credentialIssuanceStatusService.credentialIssued(credentialRequest.getCredentialConfigurationId(), issuanceTransactionId);
        };

        auditService.logIssueCredentials(credentialIssueContext, issuanceTransactionId, credentials.size(), notificationId);
        metricService.countIssued(credentialRequest.getCredentialConfigurationId());
        return CredentialResponse.builder()
                .credentials(credentials)
                .notificationId(notificationId)
                .build();
    }

    private void createSubjectCredentialTransaction(CredentialIssueContext credentialIssueContext, IssuanceTransactionId issuanceTransactionId) {
        if(credentialIssueContext.issuanceType() == CredentialIssuanceType.AUTHORIZATION_CODE) {
            subjectCredentialTransactionDao.insertSubjectCredentialTransaction(
                    credentialIssueContext.personIdentifier(),
                    issuanceTransactionId.getValue(),
                    System.currentTimeMillis()
            );
        }
    }

    private IssuanceTransactionId resolveIssuanceTransactionId(JWT accessToken, CredentialIssuanceType issuanceFlow, CredentialRequest credentialRequest, CredentialIssuerTenant tenant) {
        if (issuanceFlow == CredentialIssuanceType.AUTHORIZATION_CODE) {
            IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
            credentialIssuanceTransactionDao.insertTransaction(
                    issuanceTransactionId.getValue(),
                    credentialRequest.getCredentialConfigurationId(),
                    tenant.getId(),
                    System.currentTimeMillis()
            );

            return issuanceTransactionId;
        }

        try {
            String transactionId = accessToken.getJWTClaimsSet().getStringClaim("tx_id");
            if (transactionId != null) {
                return new IssuanceTransactionId(transactionId);
            }
        } catch (ParseException e) {
            throw new InvalidCredentialRequest("Invalid access token");
        }
        throw new InvalidCredentialRequest("Missing transactionId claim for pre-authorized code flow");
    }

}
