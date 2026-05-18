package no.idporten.eudiw.statuslist.util;

import java.util.Arrays;

public class FreeIndexList {
    private final int[] data;

    public FreeIndexList(int[] data) {
        this.data = data;
    }

    public int[] getRange(int from, int to) {
        return Arrays.copyOfRange(data, from, to);
    }
}
