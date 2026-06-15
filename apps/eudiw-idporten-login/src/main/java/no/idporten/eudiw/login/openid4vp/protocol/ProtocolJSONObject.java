package no.idporten.eudiw.login.openid4vp.protocol;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class ProtocolJSONObject extends JSONObject {

    public void appendArrayField(String key, Object value) {
        JSONArray jsonArray = (JSONArray) getOrDefault(key, new JSONArray());
        jsonArray.add(value);
        appendField(key, jsonArray);
    }

}
