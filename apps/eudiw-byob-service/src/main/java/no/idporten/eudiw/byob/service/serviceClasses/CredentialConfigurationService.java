package no.idporten.eudiw.byob.service.serviceClasses;

import no.idporten.eudiw.byob.service.exception.BadRequestException;
import no.idporten.eudiw.byob.service.model.ByobInput;
import no.idporten.eudiw.byob.service.model.ResponseTopObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CredentialConfigurationService {

    private Map<String, ByobInput> persistenceLayer = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(CredentialConfigurationService.class);
    private int counter= 0;
    private static final String PREFIX = "net.eidas2sandkasse:";
    private static final String SD_JWT_VC = "_sd_jwt_vc";

    public CredentialConfigurationService() {

    }

    /**
     * Takes in user input, sends input to check if the VCT is already registered, and if
     * not already registered, it gives back a generated id which is not used for anything
     * as well as the proof.
     *
     * @param proof user input that user POSTS in to BYOB in order to "build your own bevis"
     * @return a new HashMap with an id that consists of a set prefix, the VCT given in input,
     *  a counter and format.
     */
    public Map<String, ByobInput> getResponseModel(ByobInput proof) {
        String id = buildVct(proof);
        HashMap<String, ByobInput> response = new HashMap<>();
        response.put(id, proof);
        return response;
    }

    public String buildVct(ByobInput proof) {
        if (persistenceLayer.containsKey(proof.vct())) {
            throw new BadRequestException("Credential configuration already exists");
        }
            updatePersistenceLayer(proof);
            return PREFIX + proof.vct() + counter++ + SD_JWT_VC;
    }

    public void updatePersistenceLayer(ByobInput proof) {
        this.persistenceLayer.put(proof.vct(), proof);
    }

    public ResponseTopObject getAllEntries() {
        return new ResponseTopObject(persistenceLayer.values().stream().toList());

    }

    public ByobInput searchCredentialConfiguration(String id) {
        return persistenceLayer.get(id);
    }
}
