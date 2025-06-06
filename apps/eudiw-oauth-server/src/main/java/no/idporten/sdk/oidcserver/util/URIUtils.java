package no.idporten.sdk.oidcserver.util;

import lombok.SneakyThrows;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

import static no.idporten.sdk.oidcserver.util.StringUtils.hasText;

public class URIUtils {

    @SneakyThrows
    public static URI appendQuery(URI uri, Map<String, String> queryParams) {
        return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), serializeParameters(queryParams), null);
    }

    @SneakyThrows
    public static URI appendPath(URI uri, String path) {
        return new URI(uri.getScheme(), uri.getAuthority(), appendPaths(uri.getPath(), path), uri.getQuery(), null);
    }

    private static String appendPaths(String... paths) {
        return String.join("/", paths).replaceAll("/{2,}+", "/");
    }

    public static String serializeParameters(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        return params.entrySet()
                .stream()
                .filter(entry -> hasText(entry.getValue()))
                .map(entry -> "%s=%s".formatted(urlEncode(entry.getKey()), urlEncode(entry.getValue())))
                .collect(Collectors.joining("&"));
    }

    public static Map<String, List<String>> parseParameters(String query) {
        Map<String, List<String>> params = new HashMap();
        if (!StringUtils.hasText(query)) {
            return params;
        }
                StringTokenizer st = new StringTokenizer(query.trim(), "&");

                while(st.hasMoreTokens()) {
                    String param = st.nextToken();
                    String[] pair = param.split("=", 2);
                    String key = urlDecode(pair[0]);
                    String value = pair.length > 1 ? urlDecode(pair[1]) : "";
                    if (params.containsKey(key)) {
                        List<String> updatedValueList = new LinkedList((Collection)params.get(key));
                        updatedValueList.add(value);
                        params.put(key, Collections.unmodifiableList(updatedValueList));
                    } else {
                        params.put(key, Collections.singletonList(value));
                    }
                }
            return params;
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, Charset.forName("UTF-8"));
    }

    private static String urlDecode(String s) {
        return URLDecoder.decode(s, Charset.forName("UTF-8"));
    }

}
