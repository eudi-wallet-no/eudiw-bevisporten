package no.idporten.eudiw.issuer.openid4vci.metadata;

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
public class Display {

    @JsonProperty("name")
    private String name;

    @JsonProperty("locale")
    private String locale;

}
