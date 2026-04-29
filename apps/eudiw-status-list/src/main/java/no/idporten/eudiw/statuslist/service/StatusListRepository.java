package no.idporten.eudiw.statuslist.service;

import no.idporten.eudiw.statuslist.exceptions.StatusListNotFoundException;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class StatusListRepository {

    private final Map<String, StatusList> statusLists = new HashMap<>();
    private final StatusListProperties statusListProperties;

    public StatusListRepository(StatusListProperties statusListProperties) {
        this.statusListProperties = statusListProperties;
    }


    public StatusList getStatusList(String id) {
        if (!statusLists.containsKey(id)) {
            throw new StatusListNotFoundException(id);
        }

        return statusLists.get(id);
    }

    public StatusList generateNewStatusList() {
        String id = "%d".formatted(statusLists.size() + 1);
        StatusList statusList = new StatusList(id, statusListProperties.listSize(), statusListProperties.bitsPerStatus());
        statusLists.put(id, statusList);
        return statusList;
    }

    public void putStatusList(String id, StatusList statusList) {
        statusLists.put(id, statusList);
    }

    public StatusList getNextFreeStatusList() {
        for (StatusList statusList : statusLists.values()) {
            if (!statusList.isFull()) {
                return statusList;
            }
        }
        return generateNewStatusList();
    }
}
