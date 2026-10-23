package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;

import java.util.List;

public final class ResponseEncryption {

    public static final JWEAlgorithm ENCRYPTION_ALGORITHM = JWEAlgorithm.ECDH_ES;
    public static final List<EncryptionMethod> SUPPORTED_ENCRYPTION_METHODS = List.of(EncryptionMethod.A128GCM, EncryptionMethod.A256GCM);

    private ResponseEncryption() {
    }
}
