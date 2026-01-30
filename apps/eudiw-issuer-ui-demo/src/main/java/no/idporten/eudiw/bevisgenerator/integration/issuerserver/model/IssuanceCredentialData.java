package no.idporten.eudiw.bevisgenerator.integration.issuerserver.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class IssuanceCredentialData extends HashMap<String, Serializable> {

    public IssuanceCredentialData() {
        super();
    }

    public IssuanceCredentialData(Map<String, Serializable> claims) {
        super(claims);
    }

}
