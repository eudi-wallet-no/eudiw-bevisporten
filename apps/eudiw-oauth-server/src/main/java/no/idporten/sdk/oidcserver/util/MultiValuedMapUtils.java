package no.idporten.sdk.oidcserver.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class MultiValuedMapUtils {

    private MultiValuedMapUtils() {
    }

    public static String getFirstValue(String key, Map<String, List<String>> map) {
        if (map.containsKey(key) && !map.getOrDefault(key, Collections.emptyList()).isEmpty()) {
            return map.get(key).get(0);
        }
        return null;
    }

    public static Map<String, String> toMap(Map<String, List<String>> multiValuedMap) {
        return multiValuedMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get(0))
                );
    }
    public static Map<String, List<String>> toMultiValuedMap(Map<String,String> map) {
        return map.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> List.of(entry.getValue()))
                );
    }

    public static Map<String, List<String>> caseInsensitiveMap(Map<String, List<String>> map) {
        Map<String, List<String>> ciMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ciMap.putAll(map);
        return ciMap;
    }

}
