package no.idporten.eudiw.issuer.api;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class Endpoints {

    // OpenID4VCI endpoints
    public final static String METADATA_ENDPOINT = "/.well-known/openid-credential-issuer";
    public final static String CREDENTIAL_ENDPOINT = "/openid4vci/credential";
    public final static String NONCE_ENDPOINT = "/openid4vci/nonce";
    public final static String NOTIFICATION_ENDPOINT = "/openid4vci/notification";
    // Issuance extended API
    public final static String CREDENTIAL_ISSUANCE_TRANSACTION_ENDPOINT = "/api/v1/credential/issuance-transaction";
    public final static String CREDENTIAL_ISSUANCE_TRANSACTION_STATUS_ENDPOINT = "/api/v1/credential/issuance-transaction/{issuance_transaction_id}";
    public final static String CREATE_CREDENTIAL_OFFER_ENDPOINT = "/api/v1/credential-offer/create";
    public final static String OPENAPI_ENDPOINT = "/swagger-ui/index.html";

    /**
     * Calculate endpoint uri from credential issuer uri.
     */
    public static URI endpointURI(URI issuerUri, String path) {
        return UriComponentsBuilder.fromUri(issuerUri).path(path).build().toUri();
    }
}
