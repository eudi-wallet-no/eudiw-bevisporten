package no.idporten.eudiw.issuer.claimssource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWT;
import id.walt.mdoc.doc.MDoc;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.domain.Claim;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.logging.audit.AuditService;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.openid4vci.mdoc.MDocService;
import no.idporten.eudiw.issuer.openid4vci.protocol.*;
import no.idporten.eudiw.issuer.openid4vci.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Base64;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CredentialIssuerService {

    public static final String GRANT_TYPE_PRE_AUTHORIZED_CODE = "urn:ietf:params:oauth:grant-type:pre-authorized_code";
    public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final ClaimsSourceService claimsSourceService;
    private final AccessTokenValidationService accessTokenValidationService;
    private final MDocService mDocService;
    private final PreAuthorizationService preAuthorizationService;
    private final CredentialIssuanceStatusService credentialIssuanceStatusService;
    private final AuditService auditService;

    public StartIssuanceResponse startIssuerTransaction(StartIssuanceRequest startIssuanceRequest, JWT accessToken) {
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(startIssuanceRequest.getCredentialConfigurationId());
        if (!GRANT_TYPE_PRE_AUTHORIZED_CODE.equals(credentialConfigurationProperties.getGrantType())) {
            throw new IssuerServerException("invalid_request", "Credential configuration can only be used with the pre-authorized code flow", HttpStatus.BAD_REQUEST);
        }
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, credentialConfigurationProperties.getPreAuthorizationServer(), credentialConfigurationProperties.getScope());
        PreAuthorizedClaimsSource claimsSource = (PreAuthorizedClaimsSource) claimsSourceService.findClaimsSource(credentialConfigurationProperties.getDoctype());
        final IssuanceTransactionId issuanceTransactionId = new IssuanceTransactionId();
        final String preAuthorizedCode = preAuthorizationService.preAuthorize(issuanceTransactionId, startIssuanceRequest);
        claimsSource.preAuthorize(issuanceTransactionId.getValue(), accessToken, startIssuanceRequest.getClaimsMap());
        CredentialOffer credentialOffer = CredentialOffer.builder()
                .credentialIssuer(credentialIssuerServerProperties.getCredentialIssuer().toString())
                .credentialConfigurationId(credentialConfigurationProperties.getIdentifier())
                .grants(Grants.builder()
                        .preAuthorizedCodeGrant(PreAuthorizedCodeGrant.builder().preAuthorizedCode(preAuthorizedCode)
                                .txCode(TxCode.builder()
                                        .inputMode("numeric")
                                        .length(4)
                                        .description("Enter code from SMS to issue %s".formatted(claimsSource.getMetadata().getDisplays().getFirst().getName()))
                                        .build())
                                .build())
                        .build())
                .build();
        IssuanceStatus issuanceStatus = credentialIssuanceStatusService.offerIssued(issuanceTransactionId, startIssuanceRequest.getCredentialConfigurationId());
        auditService.logStartCredentialIssuanceTransaction(credentialOffer.getCredentialIssuer(), startIssuanceRequest.getCredentialConfigurationId(), issuanceTransactionId, accessToken);
        return StartIssuanceResponse.builder()
                .credentialOffer(credentialOffer)
                .issuanceTransactionId(issuanceTransactionId)
                .build();
    }

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

    @SneakyThrows
    public CredentialResponse issueCredentials(CredentialRequest credentialRequest, JWT accessToken) {
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(credentialRequest.getCredentialConfigurationId());
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, credentialConfigurationProperties.getAuthorizationServer(), credentialConfigurationProperties.getScope());
        JWK bindingKey = credentialRequest.getProof() != null ? credentialRequest.getProof().getBindingKey() : null;
        ClaimsSource claimsSource = claimsSourceService.findClaimsSource(credentialConfigurationProperties.getDoctype());
        List<no.idporten.eudiw.issuer.claimssource.domain.Claim> claims = claimsSource.retrieveClaims(accessToken);
        List<Credential> credentials = createCredentials(bindingKey, credentialConfigurationProperties.getFormat(), credentialConfigurationProperties.getDoctype(), claims);
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


    @SneakyThrows
    protected List<Credential> createCredentials(JWK bindingKey, String format, String docType, List<Claim> claims) {
        ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
        if ("mso_mdoc".equals(format)) {
            MDoc mDoc = mDocService.issueCredentials(bindingKey, docType, claims);
            String encodedMDoc = Base64.getUrlEncoder().encodeToString(mDoc.getIssuerSigned().toMapElement().toCBOR());
            return List.of(Credential.builder().credential(encodedMDoc).build());
        } else {
            return List.of(Credential.builder().credential(objectWriter.writeValueAsString(claims)).build());
        }
    }

}
