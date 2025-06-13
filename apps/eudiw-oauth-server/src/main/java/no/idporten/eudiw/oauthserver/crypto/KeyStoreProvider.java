package no.idporten.eudiw.oauthserver.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextException;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;

/**
 * Utility for loading a keystore using a resource loader.
 */
@Slf4j
public class KeyStoreProvider {

    private KeyStore keyStore;

    public KeyStoreProvider(KeyStoreProperties keyStoreProperties) {
        try (InputStream is = inputStreamForLocation(keyStoreProperties.location())) {
            KeyStore keystore = KeyStore.getInstance(keyStoreProperties.type());
            keystore.load(is, keyStoreProperties.password().toCharArray());
            if (log.isInfoEnabled()) {
                log.info("Loaded keystore of type {} from {}",
                        keyStoreProperties.type(),
                        keyStoreProperties.location().startsWith("base64:")
                                ? String.format("%100.100s...", keyStoreProperties.location())
                                : keyStoreProperties.location());
            }
            this.keyStore = keystore;
        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException e) {
            throw new ApplicationContextException("Failed to load keystore.", e);
        }
    }

    private InputStream inputStreamForLocation(String location) throws IOException {
        if (location.startsWith("base64:")) {
            return new ByteArrayInputStream(Base64.getDecoder().decode(location.substring(7)));
        }
        if (location.equals("file:")) {
            return new FileInputStream(location.substring(5));
        }
        if (location.startsWith("classpath:")) {
            return this.getClass().getClassLoader().getResourceAsStream(location.substring(10));
        }
        throw new IOException("Unable to load keystore from location [%s]".formatted(location));
    }

    public KeyStore keyStore() {
        return keyStore;
    }

}
