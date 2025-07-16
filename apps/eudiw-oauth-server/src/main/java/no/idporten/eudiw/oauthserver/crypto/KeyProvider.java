package no.idporten.eudiw.oauthserver.crypto;

import lombok.SneakyThrows;

import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Utility for holding keys and certificates.
 */
public class KeyProvider {

    private String alias;
    private PrivateKey privateKey;
    private Certificate certificate;
    private List<Certificate> certificateChain;

    private PublicKey publicKey;

    /**
     * Extract private key, public key and certificate(s) from a keystore.
     */
    public KeyProvider(KeyStore keyStore, String alias, String password) {
        try {
            this.alias = alias;
            privateKey = (PrivateKey) keyStore.getKey(alias, password.toCharArray());
            certificate = keyStore.getCertificate(alias);
            publicKey = certificate.getPublicKey();
            certificateChain = Arrays.asList(keyStore.getCertificateChain(alias));
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a private key representation from key bytes
     */
    public KeyProvider(String algorthm, String key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            KeyFactory keyFactory = KeyFactory.getInstance(algorthm);
            privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
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

    @SneakyThrows
    public String getKid() {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String getAlias() {
        return alias;
    }

}
