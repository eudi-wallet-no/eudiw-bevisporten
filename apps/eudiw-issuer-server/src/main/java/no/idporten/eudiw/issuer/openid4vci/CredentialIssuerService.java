package no.idporten.eudiw.issuer.openid4vci;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.CredentialCreateService;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenCredentialValidationContext;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
import no.idporten.eudiw.issuer.openid4vci.protocol.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.List;


@RequiredArgsConstructor
@Service
public class CredentialIssuerService {

    private final ClaimsSourceService claimsSourceService;
    private final AccessTokenValidationService accessTokenValidationService;

    private final CredentialCreateService credentialCreateService;
    private final CredentialIssuanceStatusService credentialIssuanceStatusService;
    private final AuditService auditService;

    @SneakyThrows
    public CredentialResponse issueCredentials(CredentialIssuerTenant tenant, CredentialRequest credentialRequest, JWT accessToken) {
        ExtendedCredentialConfiguration credentialConfiguration = tenant.findCredentialConfiguration(credentialRequest.getCredentialConfigurationId());
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, AccessTokenCredentialValidationContext.forAuthorization(credentialConfiguration));
        ClaimsSource claimsSource = claimsSourceService.findClaimsSource(credentialConfiguration.getCredentialIssuerContext().getCredentialDataSourceUri());
        List<JWK> bindingKeys = getBindingKeys(credentialRequest.getProofs(), credentialRequest.getProof());
        List<Claim> claims = claimsSource.issueClaims(new CredentialIssueContext(accessToken, tenant, credentialConfiguration.getExtendedCredentialMetadata()));
        List<Credential> credentials = credentialCreateService.createCredentials(tenant, bindingKeys, credentialConfiguration, claims);
        IssuanceTransactionId issuanceTransactionId = getIssuanceTransactionId(accessToken);
        NotificationId notificationId = credentialIssuanceStatusService.credentialIssued(credentialRequest.getCredentialConfigurationId(), issuanceTransactionId);

        auditService.logIssueCredentials(credentialConfiguration.getCredentialIssuerContext().getAuthorizationServer(), credentialRequest.getCredentialConfigurationId(), issuanceTransactionId, credentialConfiguration.getFormat(), notificationId, accessToken);
        return CredentialResponse.builder()
                .credentials(credentials)
                .notificationId(notificationId)
                .build();
    }

    // Support OpenID4VCI 1 proofs vs older versions proof
    protected List<JWK> getBindingKeys(Proofs proofs, Proof proof) {
        return proofs != null ? proofs.getBindingKeys() : proof != null ? List.of(proof.getBindingKey()) : null;
    }

    private IssuanceTransactionId getIssuanceTransactionId(JWT accessToken) {
        try {
            if (accessToken.getJWTClaimsSet().getStringClaim("tx_id") != null) {
                return new IssuanceTransactionId(accessToken.getJWTClaimsSet().getStringClaim("tx_id"));
            }
        } catch (ParseException e) {
            throw new IssuerServerException(IssuerServerException.INVALID_CREDENTIAL_REQUEST, "Invalid token", HttpStatus.BAD_REQUEST);
        }
        return null;
    }

}
