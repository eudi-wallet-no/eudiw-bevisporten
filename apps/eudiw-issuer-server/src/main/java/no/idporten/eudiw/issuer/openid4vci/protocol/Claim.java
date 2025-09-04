package no.idporten.eudiw.issuer.openid4vci.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Claim {

    private String name;
    private String value;

}
