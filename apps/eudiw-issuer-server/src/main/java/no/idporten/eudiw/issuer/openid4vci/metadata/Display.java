package no.idporten.eudiw.issuer.openid4vci.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Display information for credential and credential claims.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Display {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @Builder.Default
    @JsonProperty("locale")
    private String locale = "en";

    @JsonProperty("background_color")
    private String backgroundColor;

    @JsonProperty("text_color")
    private String textColor;

    public Display(String locale, String name) {
        this(name, null, locale, "#afcee9", "#002c54");
    }

    public Display(String locale, String name, String description) {
        this(name, description, locale, "#afcee9", "#002c54");
    }

}
