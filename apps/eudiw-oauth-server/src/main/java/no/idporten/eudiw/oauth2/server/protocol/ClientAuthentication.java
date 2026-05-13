package no.idporten.eudiw.oauth2.server.protocol;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientAuthentication implements AuditDataProvider {

    private String clientId;
    private String tokenEndpointAuthMethod;
    private String clientAttestation;
    private String clientAttestationPoP;
    private String attestationChallenge;

    @Override
    public AuditData getAuditData() {
        return AuditData.builder()
                .attribute("client_id", clientId)
                .attribute("token_endpoint_auth_method", tokenEndpointAuthMethod)
                .attribute("client_attestation", clientAttestation)
                .maskedJwt("client_attestation_pop", clientAttestationPoP)
                .attribute("attestation_challenge", attestationChallenge)
                .build();
    }

}
