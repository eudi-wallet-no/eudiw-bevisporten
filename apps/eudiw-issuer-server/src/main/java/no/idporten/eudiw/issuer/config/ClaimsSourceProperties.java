package no.idporten.eudiw.issuer.config;

import lombok.Data;
import no.idporten.eudiw.issuer.openid4vci.metadata.ClaimsDescription;
import no.idporten.eudiw.issuer.openid4vci.metadata.Display;

import java.util.List;

@Data
public class ClaimsSourceProperties {

    private String doctype;
    private List<Display> display;
    private List<ClaimsDescription> claims;

}
