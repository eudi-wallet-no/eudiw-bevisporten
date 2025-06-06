package no.idporten.sdk.oidcserver.protocol;

import lombok.*;
import no.idporten.sdk.oidcserver.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"parameters"})
@ToString(exclude = {"parameters"})
public class AuthorizationResponse implements AuditDataProvider {

    private String redirectUri;
    private String responseMode;
    private String iss;
    private String aud;
    private String state;
    private String code;
    private String error;
    private String errorDescription;

    @Getter(AccessLevel.NONE)
    @Builder.Default
    private Map<String, String> parameters = new HashMap<>();

    public Map<String, String> toResponseParameters() {
        Map<String, String> responseParameters = new HashMap<>();
        responseParameters.putAll(parameters);
        responseParameters.compute("code", (k,v) -> code);
        responseParameters.computeIfAbsent("error", (k) -> error);
        responseParameters.computeIfAbsent("error_description", (k) -> errorDescription);
        responseParameters.computeIfAbsent("state", (k) -> state);
        responseParameters.computeIfAbsent("iss", (k) -> iss);
        return responseParameters;
    }

    /**
     * Checks if response_mode is query.  query is default if response_mode is not set.
     */
    public boolean isQuery() {
        return "query".equals(responseMode) || !StringUtils.hasText(responseMode);
    }

    /**
     * Checks if response_mode is form_post.
     */
    public boolean isFormPost() {
        return "form_post".equalsIgnoreCase(responseMode);
    }

    /**
     * Checks if response_mode is query.jwt.
     */
    public boolean isQueryJwt() {
        return "query.jwt".equalsIgnoreCase(responseMode);
    }


    /**
     * Gets a custom parameter by name
     */
    public String getParameter(String parameter) {
        return parameters.get(parameter);
    }

    /**
     * Adds a custom parameter to response.
     */
    public void addParameter(String parameter, String value) {
        parameters.put(parameter, value);
    }

    @Override
    public AuditData getAuditData() {
        return AuditData.builder()
                .attribute("redirect_uri", redirectUri)
                .attribute("response_mode", responseMode)
                .attribute("iss", iss)
                .attribute("state", state)
                .attribute("code", code)
                .attribute("error", error)
                .attribute("error_description", errorDescription)
                .build();
    }

}
