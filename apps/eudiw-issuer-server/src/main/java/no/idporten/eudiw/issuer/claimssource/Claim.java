package no.idporten.eudiw.issuer.claimssource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Claim {

    private String path;
    private String value;

}
