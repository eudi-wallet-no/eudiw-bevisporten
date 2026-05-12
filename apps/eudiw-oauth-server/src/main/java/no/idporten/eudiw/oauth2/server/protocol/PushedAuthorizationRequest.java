package no.idporten.eudiw.oauth2.server.protocol;

import lombok.*;
import no.idporten.eudiw.oauth2.server.OAuth2Exception;
import no.idporten.eudiw.oauth2.server.cache.Cacheable;
import no.idporten.eudiw.oauth2.server.util.JsonUtils;
import no.idporten.eudiw.oauth2.server.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static no.idporten.eudiw.oauth2.server.util.MultiValuedMapUtils.*;

/**
 * Representation of a pushed authorization request.  Handles a core set of OIDC and OAuth2 parameters.
 * <p>
 * Extend this class to add more protocol parameters or use {@link #getParameter(String)} to retrieve parameters by name.
 */
@Getter
@EqualsAndHashCode(exclude = {"authorizationHeader", "clientSecret"})
@ToString(exclude = {"authorizationHeader", "clientSecret"})
@NoArgsConstructor
public class PushedAuthorizationRequest implements AuthenticatedRequest, ResourceIndicatorSupport, Cacheable, AuditDataProvider {

    @Serial
    private static final long serialVersionUID = 1L;

    private transient String authorizationHeader;
    private transient String clientSecret;
    private transient String clientAssertion;
    private transient String clientAssertionType;
    private transient String clientAttestation;
    private transient String clientAttestationPoP;
    private transient String dPoPHeader;
    private String clientId;
    private String redirectUri;
    private String state;
    private String nonce;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String responseType;
    private String responseMode;
    private List<String> scope;
    private List<String> acrValues;
    private List<String> uiLocales;
    private List<AuthorizationDetail> authorizationDetails;
    private String resource;
    private String issuerState;
    private transient String dpopJkt;

    @Getter(AccessLevel.NONE)
    private Map<String, String> parameters = new HashMap<>();

    @Setter
    private String resolvedAcrValue; // resolved from acr_values
    @Setter
    private String resolvedUiLocale; // resolved from ui_locales
    @Setter
    private String resolvedResponseMode; // resolved from response_mode
    @Setter
    private String resolvedDpopJkt; // resolved from param dpopJkt and header dPoP

    private long createdAtEpochMillis;
    private long expiresAtEpochMillis;

    public PushedAuthorizationRequest(final Map<String, List<String>> headers, final Map<String, List<String>> parameters) {
        Map<String, List<String>> ciHeaders = caseInsensitiveMap(headers);
        this.authorizationHeader = getFirstValue("Authorization", ciHeaders);
        this.dPoPHeader = getFirstValue("dpop", ciHeaders); // TODO feile om fleire dpop-headers
        clientId = getFirstValue("client_id", parameters);
        clientSecret = getFirstValue("client_secret", parameters);
        clientAssertion = getFirstValue("client_assertion", parameters);
        clientAssertionType = getFirstValue("client_assertion_type", parameters);
        clientAttestation = getFirstValue("OAuth-Client-Attestation", ciHeaders);
        clientAttestationPoP = getFirstValue("OAuth-Client-Attestation-PoP", ciHeaders);
        redirectUri = getFirstValue("redirect_uri", parameters);
        state = getFirstValue("state", parameters);
        nonce = getFirstValue("nonce", parameters);
        codeChallenge = getFirstValue("code_challenge", parameters);
        codeChallengeMethod = getFirstValue("code_challenge_method", parameters);
        responseType = getFirstValue("response_type", parameters);
        scope = convertSpaceDelimitedString(getFirstValue("scope", parameters));
        responseMode = getFirstValue("response_mode", parameters);
        acrValues = convertSpaceDelimitedString(getFirstValue("acr_values", parameters));
        uiLocales = convertSpaceDelimitedString(getFirstValue("ui_locales", parameters));
        authorizationDetails = convertAuthorizationDetails(getFirstValue("authorization_details", parameters));
        resource = getFirstValue("resource", parameters);
        issuerState = getFirstValue("issuer_state", parameters);
        dpopJkt = getFirstValue("dpop_jkt", parameters);
        this.parameters = toMap(parameters);
    }

    public String getParameter(String parameterName) {
        return parameters.get(parameterName);
    }

    protected List<String> convertSpaceDelimitedString(String value) {
        if (value != null && !value.isEmpty()) {
            return List.of(value.split("\\s+"));
        }
        return Collections.emptyList();
    }

    protected List<AuthorizationDetail> convertAuthorizationDetails(String value) {
        if (StringUtils.hasText(value)) {
            try {
                return JsonUtils.parseJsonArray(value).stream().map(e -> new AuthorizationDetail((Map<String, Serializable>) e)).collect(Collectors.toList());
            } catch (Exception e) {
                throw new OAuth2Exception(OAuth2Exception.INVALID_AUTHORIZATION_DETAILS, "Invalid format for authorization_details", 400, e);
            }
        }
        return Collections.emptyList();
    }

    @Override
    public void setLifetimeSeconds(long lifetimeSeconds) {
        this.createdAtEpochMillis = Instant.now().toEpochMilli();
        this.expiresAtEpochMillis = createdAtEpochMillis + (lifetimeSeconds * 1000);
    }

    @Override
    public long createdAtEpochMillis() {
        return createdAtEpochMillis;
    }

    @Override
    public long expiresAtEpochMillis() {
        return expiresAtEpochMillis;
    }

    @Override
    public void clearAuthentication() {
        clientSecret = null;
        authorizationHeader = null;
        clientAssertion = null;
        clientAttestation = null;
        clientAttestationPoP = null;
    }

    @Override
    public void setAuthenticatedClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public AuditData getAuditData() {
        Set<String> auditParameters = new HashSet<>(List.of(
                "client_id",
                "redirect_uri",
                "state",
                "nonce",
                "code_challenge",
                "code_challenge_method",
                "response_type",
                "scope",
                "response_mode",
                "acr_values",
                "ui_locales",
                "authorization_details",
                "resource",
                "issuer_state"));
        Map<String, Object> auditData = new HashMap<>();
        auditData.putAll(parameters.entrySet()
                .stream()
                .filter(entry -> auditParameters.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        return AuditData.builder().attributes(auditData).build();
    }

}
