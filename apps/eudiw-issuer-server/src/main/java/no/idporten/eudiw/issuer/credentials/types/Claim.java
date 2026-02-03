package no.idporten.eudiw.issuer.credentials.types;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Claim {

    @Singular("path")
    private List<String> path;
    private ClaimValue value;

}
