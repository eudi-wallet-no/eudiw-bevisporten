package no.idporten.eudiw.statuslist.repository;

import no.idporten.eudiw.statuslist.exceptions.ErrorCodes;
import no.idporten.eudiw.statuslist.exceptions.StatusListBadRequestException;
import no.idporten.eudiw.statuslist.exceptions.StatusListException;
import no.idporten.eudiw.statuslist.exceptions.StatusListNotFoundException;
import no.idporten.eudiw.statuslist.repository.models.AllocatedIndexDto;
import no.idporten.eudiw.statuslist.repository.models.StatusListDto;
import no.idporten.eudiw.statuslist.repository.models.StatusListWithEntriesDto;
import no.idporten.eudiw.statuslist.util.FreeIndexList;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static no.idporten.eudiw.statuslist.util.StatusListUtil.createFreeIndexList;

@Repository
@Transactional(readOnly = true)
public class StatusListRepository {

    private final MariaDbIntegration dbIntegration;
    private final Map<FreeIndexKey, FreeIndexList> freeIndexLists = new ConcurrentHashMap<>();

    public StatusListRepository(MariaDbIntegration mariaDbIntegration) {
        this.dbIntegration = mariaDbIntegration;
    }

    public StatusListWithEntriesDto getStatusList(int listId) {
        StatusListWithEntriesDto result = dbIntegration.getStatusListWithEntries(listId);

        if (result == null) {
            throw new StatusListNotFoundException(listId);
        }

        return result;
    }

    @Transactional()
    public void createStatusEntry(int listId, int index, int status) {
        if (!isStatusAllocated(listId, index)) {
            throw new StatusListBadRequestException(
                    ErrorCodes.INVALID_REQUEST,
                    "Status at index %d is not allocated in status list with id %d".formatted(index, listId)
            );
        }

        dbIntegration.createStatusListEntry(listId, index, status);
    }

    public boolean isStatusAllocated(int listId, int index) {
        StatusListDto statusList = dbIntegration.getStatusListById(listId);
        FreeIndexList freeIndexList = getOrCreateFreeIndexList(statusList);
        return freeIndexList.isAllocated(index, statusList.next());
    }

    @Transactional()
    public List<AllocatedIndexDto> allocateToStatusList(int count) {
        List<AllocatedIndexDto> allocatedIndexes = new ArrayList<>(count);
        int remaining = count;
        int counter = 0;

        while (remaining > 0) {
            counter++;
            if (counter >= 100) {
                throw new StatusListException("Failed to allocate indexes after %d attempts".formatted(counter));
            }

            StatusListDto statusList = dbIntegration.getNextFreeStatusListDtoForUpdate();
            FreeIndexList indexList = getOrCreateFreeIndexList(statusList);

            int free = statusList.size() - statusList.next();
            int take = Math.min(free, remaining);
            int from = statusList.next();
            int to = from + take;

            int[] indexes = indexList.getRange(from, to);
            remaining -= take;

            for (int i : indexes) {
                allocatedIndexes.add(new AllocatedIndexDto(statusList.id(), i));
            }

            int next = statusList.next() + take;
            dbIntegration.updateNextIndexOnStatusList(statusList.id(), next);
        }

        return allocatedIndexes;
    }

    public int getFreeListCount() {
        return dbIntegration.getFreeListCount();
    }

    public int getFullListCount() {
        return dbIntegration.getFullListCount();
    }

    private FreeIndexList getOrCreateFreeIndexList(StatusListDto statusListDto) {
        FreeIndexKey key = new FreeIndexKey(statusListDto.id(), statusListDto.size(), statusListDto.seed());
        return freeIndexLists.computeIfAbsent(
                key,
                _ -> createFreeIndexList(statusListDto.size(), statusListDto.seed())
        );
    }
}
