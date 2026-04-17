package no.idporten.eudiw.statuslist.provider;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import no.idporten.eudiw.statuslist.exceptions.StatusListNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatusProviderService {

    private final StatusProviderProperties statusProviderProperties;

    private static final List<String> existingLists = List.of("1", "2", "3");

    public StatusProviderService(StatusProviderProperties statusProviderProperties) {
        this.statusProviderProperties = statusProviderProperties;
    }

    public JWT getStatusList(String id) {
        if (!existingLists.contains(id)) {
            throw new StatusListNotFoundException(id);
        }

        Map<String, Object> statusList = new HashMap<>();
        statusList.put("bits", 1);
        statusList.put("lst", "eNrbuRgAAhcBXQ");

        Date issueTime = new Date();
        Date expirationTime = new Date(issueTime.getTime() + statusProviderProperties.valid().toMillis());

        URI uri = UriComponentsBuilder.fromUri(statusProviderProperties.uri())
                .pathSegment("lists", id)
                .build()
                .toUri();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issueTime(issueTime)
                .expirationTime(expirationTime)
                .claim("ttl", statusProviderProperties.ttl().toSeconds())
                .claim("status_list", statusList)
                .subject(uri.toString())
                .build();

        return new PlainJWT(claimsSet);
    }
}
