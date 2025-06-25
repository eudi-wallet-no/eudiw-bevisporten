package no.idporten.eudiw.issuer.openid4vci.service;

import com.nimbusds.openid.connect.sdk.Nonce;
import org.springframework.stereotype.Service;

@Service
public class NonceService {

    public Nonce generateNonce() {
        return new Nonce();
    }

}
