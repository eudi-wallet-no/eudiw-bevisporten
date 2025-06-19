package no.idporten.eudiw.issuer.claimssource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWT;
import id.walt.mdoc.doc.MDoc;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import no.idporten.eudiw.issuer.openid4vci.mdoc.MDocService;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import no.idporten.eudiw.issuer.openid4vci.protocol.CredentialRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Base64;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CredentialIssuerService {

    private final CredentialIssuerServerProperties credentialIssuerServerProperties;
    private final ClaimsSourceService claimsSourceService;
    private final MDocService mDocService;

    public List<Credential> issueCredentials(CredentialRequest credentialRequest, JWT accessToken) {
        CredentialConfigurationProperties credentialConfigurationProperties = credentialIssuerServerProperties.findCredentialConfiguration(credentialRequest.getCredentialConfigurationId());
        if (! validate(credentialConfigurationProperties.getAuthorizationServer(), accessToken)) {
            throw new IssuerServerException("invalid_token", "Invalid authorization server for credential configuration", HttpStatus.UNAUTHORIZED);
        }
        if (! validateScope(credentialConfigurationProperties.getScope(), accessToken)) {
            throw new IssuerServerException("insufficient_scope", "Invalid scope for credential configuration", HttpStatus.FORBIDDEN);
        }
        JWK bindingKey = credentialRequest.getProof() != null ? credentialRequest.getProof().getBindingKey() : null;
        ClaimsSource claimsSource = claimsSourceService.findClaimsSource(credentialConfigurationProperties.getDoctype());
        List<Claim> claims = claimsSource.retrieveClaims(accessToken);
        return createCredentials(bindingKey, credentialConfigurationProperties.getFormat(), credentialConfigurationProperties.getDoctype(), claims);
    }

    private boolean validate(String authorizationServer, JWT accessToken) {
        try {
            return authorizationServer.equals(accessToken.getJWTClaimsSet().getIssuer());
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Invalid token format", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean validateScope(String expectedScope, JWT accessToken) {
        try {
            String scope = accessToken.getJWTClaimsSet().getStringClaim("scope");
            if (StringUtils.isEmpty(scope)) {
                return false;
            }
            List<String> parsedScopes = List.of(scope.split("\\s+"));
            return parsedScopes.contains(expectedScope);
        } catch (ParseException e) {
            throw new IssuerServerException("invalid_token", "Invalid token format", HttpStatus.UNAUTHORIZED);
        }
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
