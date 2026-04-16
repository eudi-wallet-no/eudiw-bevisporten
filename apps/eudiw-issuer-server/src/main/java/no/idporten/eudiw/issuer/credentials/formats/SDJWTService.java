package no.idporten.eudiw.issuer.credentials.formats;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.impl.ECDSA;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import id.walt.sdjwt.DecoyMode;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.SDPayload;
import id.walt.sdjwt.SimpleJWTCryptoProvider;
import net.minidev.json.JSONObject;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.claimssource.exception.CredentialRequestDeniedException;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.status.CredentialStatus;
import no.idporten.eudiw.issuer.credentials.types.*;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Service;

import java.security.cert.CertificateEncodingException;
import java.security.interfaces.ECPrivateKey;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static no.idporten.eudiw.issuer.credentials.ClaimValueConverter.FORMATTER_ISO_DATE;
import static no.idporten.eudiw.issuer.credentials.ClaimValueConverter.FORMATTER_ISO_DATE_TIME;

@Service
public class SDJWTService {

    private final KeystoreManager keystoreManager;
    public final static String BINARY_DATA_PREFIX = "data:%s;base64,";

    public SDJWTService(KeystoreManager keystoreManager) {
        this.keystoreManager = keystoreManager;
    }

    public Credential issueCredential(CredentialIssueContext context, JWK jwk, List<Claim> claims, CredentialStatus credentialStatus) {
        CredentialIssuerTenant credentialIssuer = context.credentialIssuerTenant();
        ExtendedCredentialConfiguration credentialConfiguration = context.credentialConfiguration();
        try {
            return Credential.builder().credential(encode(createSDJwt(credentialIssuer, jwk, credentialConfiguration, claims, credentialStatus))).build();
        } catch (JOSEException|CertificateEncodingException e) {
            throw new CredentialRequestDeniedException(credentialConfiguration.getCredentialConfigurationId(), "Failed to issue credentials of SD-JWT format", e);
        }
    }

    protected String encode(SDJwt sdJwt) {
        return sdJwt.getJwt() + sdJwt.getDisclosures().stream().collect(Collectors.joining("~", "~", "~"));
    }

    protected SDJwt createSDJwt(CredentialIssuerTenant credentialIssuer, JWK jwk, ExtendedCredentialConfiguration credentialConfiguration, List<Claim> claims, CredentialStatus credentialStatus) throws JOSEException, CertificateEncodingException {
        KeyProvider keyProvider = keystoreManager.getKeyProvider(credentialConfiguration.getCredentialIssuerContext().getCredentialSigningKeystore());
        JWSSigner jwsSigner = new ECDSASigner((ECPrivateKey) keyProvider.privateKey());
        JWSAlgorithm jwsAlgorithm = ECDSA.resolveAlgorithm((ECPrivateKey) keyProvider.privateKey());
        SimpleJWTCryptoProvider cryptoProvider = new SimpleJWTCryptoProvider(jwsAlgorithm, jwsSigner, null);

        Date now = new Date();
        JWTClaimsSet.Builder claimsSetBuilder = new JWTClaimsSet.Builder();
        claimsSetBuilder.issuer(credentialIssuer.getCredentialIssuer().toString());
        claimsSetBuilder.issueTime(now);
        claimsSetBuilder.expirationTime(Date.from(now.toInstant().plus(credentialConfiguration.getCredentialIssuerContext().getValidityDays(), ChronoUnit.DAYS)));
        claimsSetBuilder.claim("vct", credentialConfiguration.getCredentialType());
        claimsSetBuilder.claim("_sd_alg", "sha-256");
        claimsSetBuilder.claim("cnf", new JSONObject().appendField("jwk", jwk.toPublicJWK().toJSONObject()));
        if (credentialStatus != null) {
            claimsSetBuilder.claim("status", credentialStatus.toJSONObject());
        }
        for (Claim claim : claims) {
            claimsSetBuilder.claim(claim.getPath().getLast(), convert(claim.getValue()));
        }
        JWTClaimsSet claimsSet = claimsSetBuilder.build();
        JWTClaimsSet.Builder undisclosedClaimsSetBuilder = new JWTClaimsSet.Builder(claimsSet);
        for (Claim claim : claims) {
            undisclosedClaimsSetBuilder.claim(claim.getPath().getLast(), null);
        }
        JWTClaimsSet undisclosedClaimsSet = undisclosedClaimsSetBuilder.build();
        SDPayload sdPayload = SDPayload.Companion.createSDPayload(claimsSet, undisclosedClaimsSet, DecoyMode.NONE, 42);
        return SDJwt.Companion.sign(
                sdPayload,
                cryptoProvider,
                null,
                credentialConfiguration.getFormat().formatIdentifier(),
                Map.of("x5c", List.of(Base64.getEncoder().encodeToString(keyProvider.certificate().getEncoded()))));
    }

    /**
     * Converts claim values to data types supported by JWT implementation (shaded GSON).
     *
     * @param claimValue claim value
     * @return converted value
     */
    protected Object convert(ClaimValue claimValue) {
        return switch (claimValue) {
            case StringValue c -> c.value();
            case NumberValue c -> c.value();
            case BooleanValue c -> c.value();
            case BinaryValue c -> addDataPrefix(c);
            case FullDateValue c -> c.value().format(FORMATTER_ISO_DATE);
            case DateTimeValue c -> c.value().format(FORMATTER_ISO_DATE_TIME);
            case ListValue c -> c.value().stream().map(this::convert).collect(Collectors.toList());
            case MapValue c ->
                    c.value().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> convert(e.getValue())));
            case null -> throw new IllegalArgumentException("claimValue cannot be null");
        };
    }

    /**
     * Adds data prefix with mimetype, defaults to mimetype image/png, e.g.
     * <code></>data:image/png;base64,<base64encoded-string></></code>.
     *
     * @param c BinaryValue
     * @return value to issue to wallet
     */
    private static String addDataPrefix(BinaryValue c) {

        final String dataPrefix = BINARY_DATA_PREFIX.formatted(c.mimeType() != null ? c.mimeType() : "image/png");
        return dataPrefix + c.value();
    }

}
