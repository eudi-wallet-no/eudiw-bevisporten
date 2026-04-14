package no.idporten.eudiw.issuer.credentials.status;

import net.minidev.json.JSONObject;

import java.net.URI;

public record StatusList(int index, URI uri) {

    public JSONObject toJSONObject() {
        return new JSONObject()
                .appendField("idx", index)
                .appendField("uri", uri.toString());
    }

}
