package no.idporten.eudiw.issuer.credentials.formats;

import com.nimbusds.jose.jwk.JWK;
import id.walt.mdoc.objects.document.IssuerSigned;
import id.walt.mdoc.objects.elements.IssuerSignedItem;
import id.walt.mdoc.objects.mso.Status;
import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.claimssource.CredentialIssueContext;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.status.CredentialStatus;
import no.idporten.eudiw.issuer.credentials.types.Claim;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MDocService {

    private final KeystoreManager keystoreManager;

    public MDocService(KeystoreManager keystoreManager) {
        this.keystoreManager = keystoreManager;
    }

    public Credential issueCredential(CredentialIssueContext context, JWK jwk, List<Claim> claims, CredentialStatus credentialStatus) {
        String mDoc = encode(createMDoc(jwk, context.credentialConfiguration(), claims, credentialStatus));
        return Credential.builder().credential(mDoc).build();
    }

    protected String encode(IssuerSigned issuerSigned) {
        return Base64.getUrlEncoder().encodeToString(MDocSigner.encode(issuerSigned));
    }

    protected IssuerSigned createMDoc(JWK jwk, ExtendedCredentialConfiguration credentialConfiguration, List<Claim> claims, CredentialStatus credentialStatus) {
        KeyProvider keyProvider = keystoreManager.getKeyProvider(credentialConfiguration.getCredentialIssuerContext().getCredentialSigningKeystore());

        Map<String, List<IssuerSignedItem>> namespaces = new LinkedHashMap<>();
        int digestId = 0;
        for (Claim claim : claims) {
            namespaces.computeIfAbsent(claim.getPath().getFirst(), _ -> new ArrayList<>())
                    .add(MDocSigner.issuerSignedItem(digestId++, claim.getPath().getLast(), claim.getValue()));
        }
        Instant now = Clock.systemUTC().instant().truncatedTo(ChronoUnit.SECONDS);
        // TODO hack for iOS wallet mdoc issue timestamp validation failure
        Instant signedAt = now.minus(1, ChronoUnit.MINUTES);
        Instant validUntil = now.plus(credentialConfiguration.getCredentialIssuerContext().getValidityDays(), ChronoUnit.DAYS);
        Status mdocStatus = credentialStatus != null ? MDocStatusBridge.create(credentialStatus) : null;
        try {
            return MDocSigner.sign(
                    credentialConfiguration.getCredentialType(),
                    namespaces,
                    keyProvider.privateKey(),
                    List.of((X509Certificate) keyProvider.certificate()),
                    jwk.toECKey(),
                    signedAt,
                    validUntil,
                    mdocStatus);
        } catch (ClassCastException e) {
            throw new IssuerServerException(ErrorCode.SERVER_ERROR, "Device key is not an EC key, cannot create mdoc", e);
        }
    }

}
