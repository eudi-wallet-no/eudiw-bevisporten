package no.idporten.sdk.oidcserver.protocol;

import lombok.*;
import no.idporten.sdk.oidcserver.cache.Cacheable;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authorization containing information about the authenticated user, the authentication process and related information.
 * Extend this class to store additional information.
 */
@Builder(toBuilder = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Authorization implements Cacheable, AuditDataProvider {

    @Serial
    private static final long serialVersionUID = 1L;

    @Singular("attribute")

    private Map<String, Serializable> attributes = new HashMap<>();
    private String code;
    private String nonce;
    private String codeChallenge;
    private String aud;
    private String sub;
    private String amr;
    private String acr;

    private long createdAtEpochMillis;
    private long expiresAtEpochMillis;

    @Override
    public long createdAtEpochMillis() {
        return createdAtEpochMillis;
    }

    @Override
    public long expiresAtEpochMillis() {
        return expiresAtEpochMillis;
    }

    @Override
    public void setLifetimeSeconds(long lifetimeSeconds) {
        this.createdAtEpochMillis = Instant.now().toEpochMilli();
        this.expiresAtEpochMillis = createdAtEpochMillis + (lifetimeSeconds * 1000);
    }

    public void setAuthorizationDetails(List<AuthorizationDetail> authorizationDetails) {
        getAttributes().put("authorization_details", authorizationDetails);
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public AuditData getAuditData() {
        AuditData.AuditDataBuilder builder = AuditData.builder()
                .attribute("sub", sub)
                .attribute("amr", amr)
                .attribute("acr", acr)
                .attribute("aud", aud);
        attributes.forEach(builder::attribute);
        return builder.build();
    }

}
