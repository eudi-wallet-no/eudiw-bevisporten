package no.idporten.eudiw.statuslist.provider;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class StatusProviderService {

    private final StatusProviderProperties statusProviderProperties;

    public StatusProviderService(StatusProviderProperties statusProviderProperties) {
        this.statusProviderProperties = statusProviderProperties;
    }

    public JWT getStatusList(String id) {
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
