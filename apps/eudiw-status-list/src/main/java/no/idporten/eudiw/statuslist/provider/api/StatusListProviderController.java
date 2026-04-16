package no.idporten.eudiw.statuslist.provider.api;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
public class StatusListProviderController {

    @CrossOrigin(origins = "*", methods = RequestMethod.GET)
    @GetMapping(path = "/lists/{id}", produces = "application/statuslist+jwt")
    public ResponseEntity<String> getStatusList(@PathVariable String id) throws ParseException {
        Map<String, Object> statusList = new HashMap<>();
        statusList.put("bits", 1);
        statusList.put("lst", "eNrbuRgAAhcBXQ");

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issueTime(new Date(1686920170L * 1000))
                .expirationTime(new Date(2291720170L * 1000))
                .claim("ttl", 43200L)
                .claim("status_list", statusList)
                .subject("https://status.eidas2sandkasse.dev/lists/" + id)
                .build();

        PlainJWT jwt = new PlainJWT(claimsSet);
        return ResponseEntity.ok(jwt.serialize());
    }
}
