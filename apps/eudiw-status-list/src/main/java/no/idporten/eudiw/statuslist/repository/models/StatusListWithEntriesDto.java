package no.idporten.eudiw.statuslist.repository.models;

import java.util.Map;

public record StatusListWithEntriesDto(int id, int size, int bitsPerStatus, Map<Integer, Integer> entries) {
}
