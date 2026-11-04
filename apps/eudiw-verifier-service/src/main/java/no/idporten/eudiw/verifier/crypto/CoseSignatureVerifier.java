package no.idporten.eudiw.verifier.crypto;

import id.walt.certificate.x509.JavaX509CertificateUtil;
import id.walt.cose.CoseCrypto2Kt;
import id.walt.cose.CoseSign1;
import id.walt.crypto2.CryptoRuntime;
import id.walt.crypto2.keys.Key;
import id.walt.crypto2.providers.cryptography.DefaultSoftwareKeyProvidersKt;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

/**
 * Verifies COSE_Sign1 signatures (mdoc issuerAuth, status list CWT) with the public key of a given certificate.
 */
public final class CoseSignatureVerifier {

    // ES256, ES384, ES512, PS256, PS384, PS512
    public static final Set<Integer> ALLOWED_ALGORITHMS = Set.of(-7, -35, -36, -37, -38, -39);

    private static final CryptoRuntime CRYPTO_RUNTIME =
            new CryptoRuntime(DefaultSoftwareKeyProvidersKt.defaultSoftwareKeyProviders(), List.of());

    private CoseSignatureVerifier() {
    }

    /**
     * @throws IllegalArgumentException if the algorithm is not allowed or the key cannot be used with it
     */
    public static boolean verify(CoseSign1 coseSign1, X509Certificate certificate) {
        JavaX509CertificateUtil x509 = JavaX509CertificateUtil.getDefault();
        Key key;
        try {
            key = x509.restoreSubjectPublicKey(x509.parseCertificateDerEncoded(certificate.getEncoded()), CRYPTO_RUNTIME);
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException("Unable to encode certificate", e);
        }
        Boolean valid = SuspendBridge.runBlocking((scope, continuation) ->
                CoseCrypto2Kt.verify(coseSign1, key, ALLOWED_ALGORITHMS, new byte[0], continuation));
        return Boolean.TRUE.equals(valid);
    }
}
