package no.idporten.eudiw.issuer.credentials.status;

import net.minidev.json.JSONObject;

import java.net.URI;

/**
 * Represents the credential status of credential.
 */
public record CredentialStatus(StatusList statusList) {

    public static CredentialStatus create(int index, URI uri) {
        return new CredentialStatus(new StatusList(index, uri));
    }

    public JSONObject toJSONObject() {
        return new JSONObject().appendField("status_list", statusList.toJSONObject());
    }

}
