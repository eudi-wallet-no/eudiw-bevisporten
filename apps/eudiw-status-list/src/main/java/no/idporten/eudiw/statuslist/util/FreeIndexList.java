package no.idporten.eudiw.statuslist.util;

import java.util.Arrays;

public class FreeIndexList {
    private final int[] data;

    public FreeIndexList(int[] data) {
        this.data = data;
    }

    int peek(int index) {
        return data[index];
    }

    public int[] getRange(int from, int to) {
        return Arrays.copyOfRange(data, from, to);
    }

    public boolean isAllocated(int index, int nextIndex) {
        boolean searchAllocatedSegment = nextIndex <= data.length / 2;
        int start = searchAllocatedSegment ? 0 : nextIndex;
        int end = searchAllocatedSegment ? nextIndex : data.length;

        boolean foundInSearchedSegment = contains(index, start, end);
        return searchAllocatedSegment == foundInSearchedSegment;
    }

    private boolean contains(int index, int start, int end) {
        for (int i = start; i < end; i++) {
            if (data[i] == index) {
                return true;
            }
        }
        return false;
    }
}
