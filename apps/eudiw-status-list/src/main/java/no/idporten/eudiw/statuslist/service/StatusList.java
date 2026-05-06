package no.idporten.eudiw.statuslist.service;

import no.idporten.eudiw.statuslist.exceptions.StatusListException;
import no.idporten.eudiw.statuslist.exceptions.StatusNotAllocatedException;
import no.idporten.eudiw.statuslist.exceptions.UnsupportedStatusException;

import java.util.*;

import static no.idporten.eudiw.statuslist.service.Status.VALID;

public class StatusList {
    private final String id;
    private final int listSize;
    private final IntStack freeIndexStack;
    private int bitsPerStatus;
    private boolean full = false;
    private Map<Integer, Integer> statusList = new HashMap<>();

    public StatusList(String id) {
        this.id = id;
        this.listSize = 1_000_000;
        setBitsPerStatus(1);
        freeIndexStack = createFreeIndexStack(listSize);
    }

    public StatusList(String id, int listSize, int bitsPerStatus) {
        this.id = id;
        this.listSize = listSize;
        setBitsPerStatus(bitsPerStatus);
        freeIndexStack = createFreeIndexStack(listSize);
    }

    public String getId() {
        return id;
    }

    public boolean isFull() {
        return full;
    }

    public int getStatus(int index) {
        return statusList.getOrDefault(index, VALID);
    }

    public Map<Integer, Integer> getStatusList() {
        return statusList;
    }

    public void setStatusList(Map<Integer, Integer> statusList) {
        this.statusList = statusList;
    }

    public boolean containsIndex(int index) {
        return statusList.containsKey(index);
    }

    public void addStatus(int index, int status) {
        if (!containsIndex(index)) {
            throw new StatusNotAllocatedException(id, index);
        }
        statusList.put(index, status);
    }

    public int getBitsPerStatus() {
        return bitsPerStatus;
    }

    void setBitsPerStatus(int bitsPerStatus) {
        if (bitsPerStatus < 1 || bitsPerStatus > 8) {
            throw new StatusListException("bitsPerStatus must be between 1 and 8");
        }
        this.bitsPerStatus = bitsPerStatus;
    }

    public List<Integer> allocateToStatusList(int count) {
        List<Integer> allocatedIndexes = new ArrayList<>();

        if (count >= freeIndexStack.size()) {
            full = true;
            count = freeIndexStack.size();
        }

        for (int i = 0; i < count; i++) {
            int index = freeIndexStack.pop();
            allocatedIndexes.add(index);
            statusList.put(index, VALID);
        }

        return allocatedIndexes;
    }

    public void updateStatus(int index, int status) {
        if (!Status.isSupported(status)) {
            throw new UnsupportedStatusException(status);
        }
        if (!statusList.containsKey(index)) {
            throw new StatusNotAllocatedException(id, index);
        }
        statusList.put(index, status);
    }

    private IntStack createFreeIndexStack(int count) {
        int[] numbers = new int[count];

        for (int i = 0; i < count; i++) {
            numbers[i] = i;
        }

        Random random = new Random();
        for (int i = count - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = numbers[i];
            numbers[i] = numbers[j];
            numbers[j] = temp;
        }

        return new IntStack(numbers, count);
    }
}
