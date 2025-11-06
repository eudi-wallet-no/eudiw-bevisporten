package no.idporten.eudiw.verifier.proxy.openid4vp.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Display {

    @JsonProperty("name")
    private String name;
    @JsonProperty("locale")
    private String locale;

}
