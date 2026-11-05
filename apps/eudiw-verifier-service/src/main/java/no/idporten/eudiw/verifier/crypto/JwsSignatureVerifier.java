package no.idporten.eudiw.verifier.crypto;

import id.walt.certificate.x509.JavaX509CertificateUtil;
import id.walt.crypto2.CryptoRuntime;
import id.walt.crypto2.jose.CompactJws;
import id.walt.crypto2.jose.JwsAlgorithm;
import id.walt.crypto2.keys.Key;
import id.walt.crypto2.providers.cryptography.DefaultSoftwareKeyProvidersKt;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

/**
 * Verifies compact JWS signatures (the issuer-signed JWT of an SD-JWT VC) with the public key of a given certificate.
 * <p>
 * walt.id's own signer key resolution for SD-JWT prefers a DID in iss over x5c, and falls back to .well-known.
 * The issuer key must be the certificate that is checked against the trust list, so it is resolved here instead.
 */
public final class JwsSignatureVerifier {

    public static final Set<JwsAlgorithm> ALLOWED_ALGORITHMS = Set.of(
            JwsAlgorithm.ES256, JwsAlgorithm.ES384, JwsAlgorithm.ES512,
            JwsAlgorithm.PS256, JwsAlgorithm.PS384, JwsAlgorithm.PS512);

    private static final CryptoRuntime CRYPTO_RUNTIME =
            new CryptoRuntime(DefaultSoftwareKeyProvidersKt.defaultSoftwareKeyProviders(), List.of());

    private JwsSignatureVerifier() {
    }

    /**
     * @throws IllegalArgumentException if the signature is invalid or the algorithm is not allowed
     */
    public static void verify(String compactJws, X509Certificate certificate) {
        JavaX509CertificateUtil x509 = JavaX509CertificateUtil.getDefault();
        Key key;
        try {
            key = x509.restoreSubjectPublicKey(x509.parseCertificateDerEncoded(certificate.getEncoded()), CRYPTO_RUNTIME);
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException("Unable to encode certificate", e);
        }
        SuspendBridge.runBlocking((scope, continuation) ->
                CompactJws.INSTANCE.verify(compactJws, key, ALLOWED_ALGORITHMS, continuation));
    }
}
