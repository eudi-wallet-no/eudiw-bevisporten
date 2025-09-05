package no.idporten.eudiw.issuer.api;

public class Endpoints {

    // OpenID4VCI endpoints
    public final static String METADATA_ENDPOINT = "/.well-known/openid-credential-issuer";
    public final static String CREDENTIAL_ENDPOINT = "/openid4vci/credential";
    public final static String NONCE_ENDPOINT = "/openid4vci/nonce";
    // Issuer extended API
    public final static String START_CREDENTIAL_ISSUANCE_ENDPOINT = "/api/v1/credential/start-issuance-transaction";
    public final static String OPENAPI_ENDPOINT = "/swagger-ui/index.html";

}
