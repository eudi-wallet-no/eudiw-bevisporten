package no.idporten.eudiw.byob.service.model.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ExampleCredentialDataData extends HashMap<String, Serializable> {

    public ExampleCredentialDataData(Map exampleClaimData) {
        super(exampleClaimData);
    }

    public ExampleCredentialDataData() {
        super();
    }

}
