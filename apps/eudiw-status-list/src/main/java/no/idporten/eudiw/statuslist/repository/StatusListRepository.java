package no.idporten.eudiw.statuslist.repository;

import no.idporten.eudiw.statuslist.exceptions.StatusListNotFoundException;
import no.idporten.eudiw.statuslist.service.StatusList;
import no.idporten.eudiw.statuslist.service.StatusListProperties;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class StatusListRepository {

    private final Map<String, StatusList> statusLists = new HashMap<>();
    private final StatusListProperties statusListProperties;

    public StatusListRepository(StatusListProperties statusListProperties) {
        // TODO: Get statuslist from database
        // TODO: if no free statuslist: create new
        // TODO: create free index list based on seed
        this.statusListProperties = statusListProperties;
        generateNewStatusList();
    }

    public int getStatus(String listId, int index) {
        // TODO: select * from Status where status_list_id = listId and index = index
       return getStatusList(listId).getStatus(index);
    }

    public StatusList getStatusList(String listId) {
        // TODO: Check cache
        // TODO: If cache invalid / empty: Get statuslist from database
        // TODO: List<Status> = select * from Status where status_list_id = listId
        // TODO: Generate full statuslist
        if (!statusLists.containsKey(listId)) {
            throw new StatusListNotFoundException(listId);
        }

        return statusLists.get(listId);
    }

    public StatusList generateNewStatusList() {
        // TODO: Create new seed
        // TODO: Crete new StatusList(String id, int size, int seed, int next)
        String id = "%d".formatted(statusLists.size());
        StatusList statusList = new StatusList(id, statusListProperties.listSize(), statusListProperties.bitsPerStatus());
        statusLists.put(id, statusList);
        return statusList;
    }

    public void putStatusList(String id, StatusList statusList) {
        // TODO: DELETE
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

    public void createOrUpdateStatus(String listId, int index, int status) {
        // TODO: insert into status (status_list_id, index, status)
        // TODO: values (listId, index, status)
        // TODO: on conflict (status_list_id, index)
        // TODO: do update set status = status
    }

    public void checkStatusIsAllocated(String listId, int index) {
        // TODO: select next from status_list where id = listId
        // TODO: Check if index is allocated from free index list
    }
}
