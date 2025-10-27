package no.idporten.eudiw.issuer.openid4vci;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.ClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimsSourceService;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.issuance.status.CredentialIssuanceStatusService;
import no.idporten.eudiw.issuer.issuance.preauth.IssuanceTransactionId;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.oauth2.preauth.PreAuthorizationService;
import no.idporten.eudiw.issuer.openid4vci.mdoc.MDocService;
import no.idporten.eudiw.issuer.openid4vci.notification.NotificationId;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialRequest;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialResponse;
import no.idporten.eudiw.issuer.openid4vci.sdjwt.SDJWTService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.List;


@RequiredArgsConstructor
@Service
public class CredentialIssuerService {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final ClaimsSourceService claimsSourceService;
    private final AccessTokenValidationService accessTokenValidationService;
    private final MDocService mDocService;
    private final SDJWTService sdjwtService;
    private final PreAuthorizationService preAuthorizationService;
    private final CredentialIssuanceStatusService credentialIssuanceStatusService;
    private final AuditService auditService;

    @SneakyThrows
    public CredentialResponse issueCredentials(CredentialRequest credentialRequest, JWT accessToken) {
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(credentialRequest.getCredentialConfigurationId());
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, credentialConfigurationProperties.getAuthorizationServer(), credentialConfigurationProperties.getScope());
        JWK bindingKey = credentialRequest.getProof() != null ? credentialRequest.getProof().getBindingKey() : null;
        ClaimsSource claimsSource = claimsSourceService.findClaimsSource(credentialConfigurationProperties.getCredentialType());
        List<Claim> claims = claimsSource.retrieveClaims(accessToken);
        List<Credential> credentials = createCredentials(bindingKey, credentialConfigurationProperties, claims);
        IssuanceTransactionId issuanceTransactionId = getIssuanceTransactionId(accessToken);
        NotificationId notificationId = credentialIssuanceStatusService.credentialIssued(credentialRequest.getCredentialConfigurationId(), issuanceTransactionId);

        auditService.logIssueCredentials(credentialConfigurationProperties.getAuthorizationServer(), credentialRequest.getCredentialConfigurationId(), issuanceTransactionId, credentialConfigurationProperties.getFormat(), notificationId, accessToken);
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
            throw new IssuerServerException(IssuerServerException.INVALID_CREDENTIAL_REQUEST, "Invalid token", HttpStatus.BAD_REQUEST);
        }
        return null;
    }


    /**
     * Creates credentials in the format configured on credential configuration.
     */
    @SneakyThrows
    protected List<Credential> createCredentials(JWK bindingKey, CredentialConfigurationProperties credentialConfigurationProperties, List<Claim> claims) {
        return switch (credentialConfigurationProperties.getFormat()) {
            case MSO_MDOC ->
                    List.of(mDocService.issueCredential(bindingKey, credentialConfigurationProperties, claims));
            case SD_JWT_VC ->
                    List.of(sdjwtService.issueCredential(bindingKey, credentialConfigurationProperties, claims));
            case JSON_DEBUG ->
                    List.of(Credential.builder().credential(new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(claims)).build());
        };
    }

}
