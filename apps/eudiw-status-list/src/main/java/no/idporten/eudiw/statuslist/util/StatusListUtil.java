package no.idporten.eudiw.statuslist.util;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

public class StatusListUtil {
    public static String getListId(URI uri) {
        String[] parts = uri.getPath().split("/");
        return parts[parts.length - 1];
    }

    public static URI buildUri(String baseUri, String listId) {
        return UriComponentsBuilder.fromUriString(baseUri).buildAndExpand(listId).toUri();
    }
}
