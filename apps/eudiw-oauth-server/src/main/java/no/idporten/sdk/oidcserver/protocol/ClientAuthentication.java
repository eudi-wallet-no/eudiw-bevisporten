package no.idporten.sdk.oidcserver.protocol;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientAuthentication implements AuditDataProvider {

    private String clientId;
    private String tokenEndpointAuthMethod;

    @Override
    public AuditData getAuditData() {
        return AuditData.builder()
                .attribute("client_id", clientId)
                .attribute("token_endpoint_auth_method", tokenEndpointAuthMethod)
                .build();
    }

}
