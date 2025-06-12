package no.idporten.sdk.oidcserver;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility to build parameter and header maps.
 */
@Getter
public class MockRequest {

    private Map<String, List<String>> parameters = new HashMap<>();
    private Map<String, List<String>> headers = new HashMap<>();

    public MockRequest addParameter(String parameter, String value) {
        return addToMap(parameter, value, parameters);
    }

    public MockRequest setParameter(String parameter, String value) {
        List<String> values = new ArrayList<>();
        values.add(value);
        parameters.put(parameter, values);
        return this;
    }

    public MockRequest addHeader(String header, String value) {
        return addToMap(header, value, headers);
    }

    private MockRequest addToMap(String key, String value, Map<String, List<String>> map) {
        if (map.containsKey(key)) {
            map.get(key).add(value);
        } else {
            List<String> values = new ArrayList<>();
            values.add(value);
            map.put(key, values);
        }
        return this;
    }

}
