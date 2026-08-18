package no.idporten.eudiw.statuslist.logging.audit;

import no.idporten.eudiw.statuslist.domain.StatusEntry;

import java.util.*;

public record AuditableStatusEntryCollection(List<StatusEntry> entries) implements Auditable<AuditLogEntries<Map<String, Object>>> {
    @Override
    public AuditLogEntries<Map<String, Object>> toAudit() {
        Map<String, List<Integer>> statusListEntries = new HashMap<>();

        for (StatusEntry e : entries) {
            statusListEntries.computeIfAbsent(e.uri().toString(), _ -> new java.util.ArrayList<>()).add(e.idx());
        }

        int total = 0;

        List<Map<String, Object>> auditEntries = new ArrayList<>();
        for (String k : statusListEntries.keySet()) {
            List<Integer> v = statusListEntries.get(k);

            Map<String, Object> map = new HashMap<>();
            map.put("uri", k);
            map.put("indexes", v);
            map.put("count", v.size());

            total += v.size();
            auditEntries.add(map);
        }

        return new AuditLogEntries<>(
                auditEntries.stream().sorted(Comparator.comparing(m -> (String) m.get("uri"))).toList(),
                total
        );
    }
}
