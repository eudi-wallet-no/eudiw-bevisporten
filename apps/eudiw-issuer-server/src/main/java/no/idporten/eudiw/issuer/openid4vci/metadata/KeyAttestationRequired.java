package no.idporten.eudiw.issuer.openid4vci.metadata;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class KeyAttestationRequired {

    @JsonProperty("key_storage")
    private List<String> keyStorage;

    @JsonProperty("user_authentication")
    private List<String> userAuthentication;

}
