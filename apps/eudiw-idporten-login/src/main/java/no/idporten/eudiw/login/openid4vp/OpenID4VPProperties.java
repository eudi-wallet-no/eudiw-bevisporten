package no.idporten.eudiw.login.openid4vp;

import no.idporten.eudiw.login.openid4vp.verifier.model.TrustedAuthorityQuery;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "eudiw-idporten-login.openid4vp")
public record OpenID4VPProperties(String trustedAuthorities) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public List<TrustedAuthorityQuery> parseTrustedAuthorities() {
        if (!StringUtils.hasText(trustedAuthorities)) {
            return List.of();
        }

        JsonNode authoritiesNode;
        try {
            authoritiesNode = OBJECT_MAPPER.readTree(trustedAuthorities);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("TRUSTED_AUTHORITIES must be a valid JSON array", e);
        }

        if (authoritiesNode == null || !authoritiesNode.isArray() || authoritiesNode.isEmpty()) {
            throw new IllegalArgumentException("TRUSTED_AUTHORITIES must be a non-empty JSON array");
        }

        List<TrustedAuthorityQuery> authorities = new ArrayList<>();
        for (JsonNode authorityNode : authoritiesNode) {
            if (!authorityNode.isObject()) {
                throw new IllegalArgumentException(
                        "Each TRUSTED_AUTHORITIES entry must contain a non-empty type and non-empty values array");
            }

            JsonNode typeNode = authorityNode.get("type");
            JsonNode valuesNode = authorityNode.get("values");
            if (typeNode == null
                    || !typeNode.isTextual()
                    || !StringUtils.hasText(typeNode.textValue())
                    || valuesNode == null
                    || !valuesNode.isArray()
                    || valuesNode.isEmpty()) {
                throw new IllegalArgumentException(
                        "Each TRUSTED_AUTHORITIES entry must contain a non-empty type and non-empty values array");
            }

            List<String> values = new ArrayList<>();
            for (JsonNode valueNode : valuesNode) {
                if (!valueNode.isTextual() || !StringUtils.hasText(valueNode.textValue())) {
                    throw new IllegalArgumentException(
                            "Each TRUSTED_AUTHORITIES entry must contain a non-empty type and non-empty values array");
                }
                values.add(valueNode.textValue());
            }
            authorities.add(new TrustedAuthorityQuery(typeNode.textValue(), List.copyOf(values)));
        }

        return List.copyOf(authorities);
    }
}
