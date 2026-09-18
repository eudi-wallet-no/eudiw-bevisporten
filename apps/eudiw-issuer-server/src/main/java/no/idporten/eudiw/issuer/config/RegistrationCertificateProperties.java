package no.idporten.eudiw.issuer.config;

import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Configuration for the registration certificate associated with a tenant.
 *
 * @param enabled whether registration certificate handling is enabled
 * @param registrationCertificateJwt the registration certificate as a JWT
 * @param registrarDataset the registrar dataset associated with the certificate, as a JSON object
 */
public record RegistrationCertificateProperties(
        @DefaultValue("false") boolean enabled,
        String registrationCertificateJwt,
        String registrarDataset) {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builderWithJackson2Defaults().build();

    /**
     * Validates that the registration certificate JWT and registrar dataset are present and well-formed
     * when registration certificate handling is enabled. Intended to be called at startup so that
     * misconfiguration fails fast instead of surfacing later during scheduled metadata refresh.
     *
     * @throws IllegalArgumentException if enabled but the JWT is missing or the registrar dataset is missing or invalid
     */
    public void validate() {
        if (!enabled) {
            return;
        }
        if (!StringUtils.hasText(registrationCertificateJwt)) {
            throw new IllegalArgumentException("Registration certificate JWT must be configured when registration certificate is enabled");
        }
        registrarDatasetJson();
    }

    /**
     * Parses the registrar dataset as a JSON object for inclusion in metadata.
     *
     * @return the registrar dataset as a JSON object, or {@code null} if registration certificate handling is disabled
     * @throws IllegalArgumentException if the dataset is missing, not valid JSON, or not an object
     */
    public ObjectNode registrarDatasetJson() {
        if (!enabled) {
            return null;
        }
        if (!StringUtils.hasText(registrarDataset)) {
            throw new IllegalArgumentException("Registrar dataset must be configured when registration certificate is enabled");
        }
        try {
            JsonNode jsonNode = JSON_MAPPER.readTree(registrarDataset);
            if (jsonNode instanceof ObjectNode objectNode) {
                return objectNode;
            }
            throw new IllegalArgumentException("Registrar dataset must be a JSON object");
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Registrar dataset must be valid JSON", e);
        }
    }

}

