package no.idporten.eudiw.verifier.statuslist;

import org.springframework.http.MediaType;

public enum StatusListTokenFormat {
    JWT("application/statuslist+jwt"),
    CWT("application/statuslist+cwt");

    private final MediaType mediaType;

    StatusListTokenFormat(String mediaType) {
        this.mediaType = MediaType.parseMediaType(mediaType);
    }

    public MediaType mediaType() {
        return mediaType;
    }

    public StatusListTokenFormat fallback() {
        return this == JWT ? CWT : JWT;
    }
}
