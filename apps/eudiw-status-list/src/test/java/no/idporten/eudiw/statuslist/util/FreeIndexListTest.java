package no.idporten.eudiw.statuslist.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FreeIndexListTest {
    @Test
    @DisplayName("Should return correct numbers")
    void shouldReturnCorrectNumbers() {
       FreeIndexList list = getShuffledList();
       int[] slice = list.getRange(0, 5);

       assertEquals(5, slice.length);
       assertEquals(9, slice[0]);
       assertEquals(5, slice[1]);
       assertEquals(7, slice[2]);
       assertEquals(6, slice[3]);
       assertEquals(1, slice[4]);
    }

    @Test
    @DisplayName("Should assert the index is allocated")
    void shouldAssertIndexIsAllocated() {
        FreeIndexList list = getShuffledList();
        assertFalse(list.isAllocated(3, 5));
        assertFalse(list.isAllocated(0, 5));
        assertTrue(list.isAllocated(1, 5));
    }

    private FreeIndexList getShuffledList() {
        return StatusListUtil.createFreeIndexList(10, 42);
    }
}
