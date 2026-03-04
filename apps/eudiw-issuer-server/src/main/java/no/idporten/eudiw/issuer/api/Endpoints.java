package no.idporten.eudiw.issuer.api;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * Endpoint URI constants for root and tenant specific endpoints.
 */
public class Endpoints {

    // OpenID4VCI endpoints
    public final static String METADATA_ENDPOINT = "/.well-known/openid-credential-issuer";
    public final static String METADATA_ENDPOINT_TENANT = "/.well-known/openid-credential-issuer/{tenant}";
    public final static String CREDENTIAL_ENDPOINT = "/openid4vci/credential";
    public final static String CREDENTIAL_ENDPOINT_TENANT = "/{tenant}/openid4vci/credential";
    public final static String NONCE_ENDPOINT = "/openid4vci/nonce";
    public final static String NONCE_ENDPOINT_TENANT = "/{tenant}/openid4vci/nonce";
    public final static String NOTIFICATION_ENDPOINT = "/openid4vci/notification";
    public final static String NOTIFICATION_ENDPOINT_TENANT = "/{tenant}/openid4vci/notification";
    // Issuance extended API
    public final static String CREDENTIAL_ISSUANCE_TRANSACTION_ENDPOINT = "/api/v1/credential/issuance-transaction";
    public final static String CREDENTIAL_ISSUANCE_TRANSACTION_ENDPOINT_TENANT = "/{tenant}/api/v1/credential/issuance-transaction";
    public final static String CREDENTIAL_ISSUANCE_TRANSACTION_STATUS_ENDPOINT = "/api/v1/credential/issuance-transaction/{issuance_transaction_id}";
    public final static String CREDENTIAL_ISSUANCE_TRANSACTION_STATUS_ENDPOINT_TENANT = "/{tenant}/api/v1/credential/issuance-transaction/{issuance_transaction_id}";
    public final static String CREATE_CREDENTIAL_OFFER_ENDPOINT = "/api/v1/credential-offer/create";
    public final static String CREATE_CREDENTIAL_OFFER_ENDPOINT_TENANT = "/{tenant}/api/v1/credential-offer/create";
    public final static String OPENAPI_ENDPOINT = "/swagger-ui/index.html";
    // Tenant variable
    public final static String TENANT_PATH_VARIABLE = "tenant";

    // TODO root handling når konfigurasjon for tenants innføres
    /**
     * Calculate endpoint uri from credential issuer uri, path and tenant.  Specially handles root...
     */
    public static URI endpointURI(URI issuerUri, String path, String tenant) {
        return UriComponentsBuilder.fromUri(
                        UriComponentsBuilder.fromUri(issuerUri)
                                .pathSegment(path.split("/"))
                                .uriVariables(Map.of(TENANT_PATH_VARIABLE, tenant == null ||"root".equals(tenant) ? "" : tenant))
                                .build()
                                .toUri())
                .build()
                .toUri();
    }

}
