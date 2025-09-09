package no.idporten.eudiw.issuer.ui.issuer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "issuer-ui.issuer-server")
public class IssuerServerProperties {


    private String baseUrl;
    private String issuanceEndpoint;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getIssuanceEndpoint() {
        return issuanceEndpoint;
    }

    public void setIssuanceEndpoint(String issuanceEndpoint) {
        this.issuanceEndpoint = issuanceEndpoint;
    }

    public String getIssuanceUrl() {
        return baseUrl + issuanceEndpoint;
    }

}
