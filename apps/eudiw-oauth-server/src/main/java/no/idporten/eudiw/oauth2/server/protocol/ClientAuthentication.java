package no.idporten.eudiw.oauth2.server.protocol;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class ClientAuthentication implements AuditDataProvider, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String clientId;
    private String tokenEndpointAuthMethod;
    private String clientAttestation;
    private transient String clientAttestationPoP;
    private String attestationChallenge;
    private String walletName;
    private String walletLink;

    /**
     * The Wallet Instance Attestation (WIA) used for attestation based client authentication.
     *
     * @return the WIA, or null if the client was not authenticated with an attestation
     */
    public String getWia() {
        return clientAttestation;
    }

    /**
     * Excludes the attestation PoP.
     */
    @Override
    public String toString() {
        return "ClientAuthentication(clientId=" + clientId
                + ", tokenEndpointAuthMethod=" + tokenEndpointAuthMethod
                + ", clientAttestation=" + clientAttestation
                + ", attestationChallenge=" + attestationChallenge
                + ", walletName=" + walletName
                + ", walletLink=" + walletLink + ")";
    }

    @Override
    public AuditData getAuditData() {
        return AuditData.builder()
                .attribute("client_id", clientId)
                .attribute("token_endpoint_auth_method", tokenEndpointAuthMethod)
                .attribute("client_attestation", clientAttestation)
                .maskedJwt("client_attestation_pop", clientAttestationPoP)
                .attribute("attestation_challenge", attestationChallenge)
                .attribute("wallet_name", walletName)
                .attribute("wallet_link", walletLink)
                .build();
    }

}
