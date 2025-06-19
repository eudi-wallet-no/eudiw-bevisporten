package no.idporten.eudiw.issuer.crypto;

import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * Utility for holding keys and certificates.
 */
public class KeyProvider {

    private PrivateKey privateKey;
    private Certificate certificate;
    private List<Certificate> certificateChain;

    private PublicKey publicKey;

    /**
     * Extract private key, public key and certificate(s) from a keystore.
     */
    public KeyProvider(KeyStore keyStore, String alias, String password) {
        try {
            privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            certificate = keyStore.getCertificate(alias);
            publicKey = certificate.getPublicKey();
            certificateChain = Arrays.asList(keyStore.getCertificateChain(alias));
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public PrivateKey privateKey() {
        return privateKey;
    }

    public PublicKey publicKey() {
        return publicKey;
    }

    public List<Certificate> certificateChain() {
        return certificateChain;
    }

    public X509Certificate certificate() {
        return (X509Certificate) certificate;
    }

}
