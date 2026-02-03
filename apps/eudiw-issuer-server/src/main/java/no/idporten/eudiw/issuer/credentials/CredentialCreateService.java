package no.idporten.eudiw.issuer.credentials;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialConfigurationProperties;
import no.idporten.eudiw.issuer.credentials.formats.MDocService;
import no.idporten.eudiw.issuer.credentials.formats.SDJWTService;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CredentialCreateService {

    private final MDocService mDocService;
    private final SDJWTService sdjwtService;

    @Autowired
    public CredentialCreateService(MDocService mDocService, SDJWTService sdjwtService) {
        this.mDocService = mDocService;
        this.sdjwtService = sdjwtService;
    }

    /**
     * Creates credentials in the format configured on credential configuration.
     */
    public List<Credential> createCredentials(List<JWK> bindingKeys, CredentialConfigurationProperties credentialConfigurationProperties, List<Claim> claims) {
        if (bindingKeys == null) {
            // TODO this is most likely invalid - test with android and ios updated to OpenID4VCI 1!
            return List.of(createCredential(null, credentialConfigurationProperties, claims));
        }
        return bindingKeys.stream().map(bindingKey -> createCredential(bindingKey, credentialConfigurationProperties, claims)).toList();
    }

    private Credential createCredential(JWK bindingKey, CredentialConfigurationProperties credentialConfigurationProperties, List<Claim> claims) {
        return switch (credentialConfigurationProperties.getFormat()) {
            case MSO_MDOC -> mDocService.issueCredential(bindingKey, credentialConfigurationProperties, claims);
            case SD_JWT_VC -> sdjwtService.issueCredential(bindingKey, credentialConfigurationProperties, claims);
            case JSON_DEBUG -> issueCredentialJsonDebug(claims);
        };
    }

    private static Credential issueCredentialJsonDebug(List<Claim> claims) {
        try {
            return Credential.builder().credential(new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(claims)).build();
        } catch (JsonProcessingException e) {
            throw new IssuerServerException("Failed to issue credentials of format JSON_DEBUG for claims %s".formatted(claims), "Failed to issue credentials of format JSON_DEBUG", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
