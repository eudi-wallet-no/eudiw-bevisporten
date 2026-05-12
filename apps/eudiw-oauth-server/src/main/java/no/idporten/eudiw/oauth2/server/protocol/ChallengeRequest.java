package no.idporten.eudiw.oauth2.server.protocol;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

import static no.idporten.eudiw.oauth2.server.util.MultiValuedMapUtils.toMap;

@Getter
@EqualsAndHashCode(exclude = {"headers"})
@ToString(exclude = {"headers"})
public class ChallengeRequest {

    @Getter(AccessLevel.NONE)
    private final Map<String, String> headers;

    public ChallengeRequest(final Map<String, List<String>> headers) {
        this.headers = toMap(headers);
    }

    public String getHeader(String headerName) {
        return headers.get(headerName);
    }

}
