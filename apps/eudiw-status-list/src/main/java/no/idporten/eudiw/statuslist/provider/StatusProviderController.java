package no.idporten.eudiw.statuslist.provider;

import com.nimbusds.jwt.JWT;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class StatusProviderController {

    private final StatusProviderService statusProviderService;

    public StatusProviderController(StatusProviderService statusProviderService) {
        this.statusProviderService = statusProviderService;
    }

    @CrossOrigin(origins = "*", methods = RequestMethod.GET)
    @GetMapping(path = "/lists/{id}", produces = "application/statuslist+jwt")
    public ResponseEntity<String> getStatusList(@PathVariable String id) {
        JWT jwt = statusProviderService.getStatusList(id);
        return ResponseEntity.ok(jwt.serialize());
    }
}
