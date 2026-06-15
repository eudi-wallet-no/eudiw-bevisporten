package no.idporten.eudiw.login.openid4vp;

import lombok.Data;

import java.util.List;

@Data
public class CredentialConfig {

    String format = "mso_mdoc";

    String docType = "eu.europa.ec.eudi.pid.1";

    List<String> fields = List.of("personal_administrative_number");

    List<String> path = List.of("eu.europa.ec.eudi.pid.1", "personal_administrative_number");

}
