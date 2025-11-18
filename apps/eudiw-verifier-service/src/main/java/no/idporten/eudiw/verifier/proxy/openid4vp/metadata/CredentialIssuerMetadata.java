package no.idporten.eudiw.verifier.proxy.openid4vp.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.oauth2.sdk.GeneralException;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import lombok.Data;
import net.minidev.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialIssuerMetadata {

    @JsonProperty("credential_issuer")
    private String issuer;

    @JsonProperty("credential_configurations_supported")
    private Map<String, CredentialConfiguration> credentialConfigurations;

    public CredentialConfiguration findCredentialConfigurationById(String credentialConfigurationId) {
        return credentialConfigurations.get(credentialConfigurationId);
    }

    public CredentialConfiguration findCredentialConfigurationByDoctype(String doctype) {
        return credentialConfigurations.values().stream()
                .filter(cc -> Objects.equals(cc.getDoctype(), doctype))
                .findFirst()
                .orElse(null);
    }

    public CredentialConfiguration findCredentialConfigurationByVct(String vct) {
        return credentialConfigurations.values().stream()
                .filter(cc -> Objects.equals(cc.getVct(), vct))
                .findFirst()
                .orElse(null);
    }

    public static URL resolveURL(final String issuer) throws GeneralException {

        try {
            URL issuerURL = new URL(issuer);
            if (issuerURL.getQuery() != null && !issuerURL.getQuery().trim().isEmpty()) {
                throw new RuntimeException("The issuer must not contain a query component");
            }

            if (issuerURL.getPath() != null && issuerURL.getPath().endsWith("/")) {
                return new URL(issuerURL + ".well-known/openid-credential-issuer");
            } else {
                return new URL(issuerURL + "/.well-known/openid-credential-issuer");
            }

        } catch (MalformedURLException e) {
            throw new GeneralException("The issuer is not a valid URL", e);
        }
    }

    public static CredentialIssuerMetadata parse(final JSONObject jsonObject)
            throws ParseException, JsonProcessingException {
        return new ObjectMapper().readValue(jsonObject.toString(), CredentialIssuerMetadata.class);
    }


    public static CredentialIssuerMetadata resolve(String issuer) throws Exception {
        URL configURL = resolveURL(issuer);
        HTTPRequest httpRequest = new HTTPRequest(HTTPRequest.Method.GET, configURL);
        HTTPResponse httpResponse = httpRequest.send();
        if (httpResponse.getStatusCode() != 200) {
            throw new IOException("Couldn't download credential issuer metadata from " + configURL +
                    ": Status code " + httpResponse.getStatusCode());
        }

        JSONObject jsonObject = httpResponse.getBodyAsJSONObject();
        return parse(jsonObject);
    }

}
