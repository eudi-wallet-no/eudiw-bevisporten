package no.idporten.eudiw.issuer.claimssource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWT;
import id.walt.mdoc.doc.MDoc;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.oauth2.AccessTokenValidationService;
import no.idporten.eudiw.issuer.openid4vci.mdoc.MDocService;
import no.idporten.eudiw.issuer.openid4vci.protocol.*;
import no.idporten.eudiw.issuer.openid4vci.service.IssuerTransactionId;
import no.idporten.eudiw.issuer.openid4vci.service.PreAuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

    public StartIssuanceResponse startIssuerTransaction(StartIssuanceRequest startIssuanceRequest, JWT accessToken) {
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(startIssuanceRequest.getCredentialConfigurationId());
        if (! GRANT_TYPE_PRE_AUTHORIZED_CODE.equals(credentialConfigurationProperties.getGrantType())) {
            throw new IssuerServerException("invalid_request", "Credential configuration cannot be used with the pre-authorized code flow", HttpStatus.BAD_REQUEST);
        }
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, credentialConfigurationProperties.getPreAuthorizationServer(), credentialConfigurationProperties.getScope());
        PreAuthorizedClaimsSource claimsSource = (PreAuthorizedClaimsSource) claimsSourceService.findClaimsSource(credentialConfigurationProperties.getDoctype());
        IssuerTransactionId issuerTransactionId = preAuthorizationService.generateIssuerTransactionCode();
        String preAuthorizedCode = preAuthorizationService.preAuthorize(issuerTransactionId, startIssuanceRequest);
        claimsSource.store(issuerTransactionId.getValue(), startIssuanceRequest.getClaimsMap());
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
        return StartIssuanceResponse.builder()
                .credentialOffer(credentialOffer)
                .issuerTransactionId(issuerTransactionId.getValue())
                .build();
    }

    public CredentialOffer createCredentialOffer(String credentialConfigurationId) {
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(credentialConfigurationId);
        if (! GRANT_TYPE_AUTHORIZATION_CODE.equals(credentialConfigurationProperties.getGrantType())) {
            throw new IssuerServerException("invalid_request", "Credential configuration cannot be used with the authorization code flow", HttpStatus.BAD_REQUEST);
        }
        return CredentialOffer.builder()
                .credentialIssuer(credentialIssuerServerProperties.getCredentialIssuer().toString())
                .credentialConfigurationId(credentialConfigurationProperties.getIdentifier())
                .grants(Grants.builder().authorizedCodeGrant(AuthorizedCodeGrant.builder().build()).build())
                .build();
    }

    public List<Credential> issueCredentials(CredentialRequest credentialRequest, JWT accessToken) {
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(credentialRequest.getCredentialConfigurationId());
        accessTokenValidationService.validateAccessTokenForCredentialConfiguration(accessToken, credentialConfigurationProperties.getAuthorizationServer(), credentialConfigurationProperties.getScope());
        JWK bindingKey = credentialRequest.getProof() != null ? credentialRequest.getProof().getBindingKey() : null;
        ClaimsSource claimsSource = claimsSourceService.findClaimsSource(credentialConfigurationProperties.getDoctype());
        List<Claim> claims = claimsSource.retrieveClaims(accessToken);
        return createCredentials(bindingKey, credentialConfigurationProperties.getFormat(), credentialConfigurationProperties.getDoctype(), claims);
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
