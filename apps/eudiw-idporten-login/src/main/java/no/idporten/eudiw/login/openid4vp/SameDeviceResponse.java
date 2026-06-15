package no.idporten.eudiw.login.openid4vp;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SameDeviceResponse {

    @JsonProperty("redirect_uri")
    private String redirectUri;

    public SameDeviceResponse(String redirectUri) {
        this.redirectUri = redirectUri;
    }

}
