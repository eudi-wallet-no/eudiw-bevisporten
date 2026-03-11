package no.idporten.eudiw.connector.authoritativsources.api;

import java.util.HashMap;

public class CredentialData extends HashMap<String, String> {
    public static CredentialData of(String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Arguments must be key-value pairs");
        }
        CredentialData data = new CredentialData();
        for (int i = 0; i < keyValues.length; i += 2) {
            data.put(keyValues[i], keyValues[i + 1]);
        }
        return data;
    }
}
