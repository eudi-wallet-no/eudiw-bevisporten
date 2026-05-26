package no.idporten.eudiw.statuslist.repository;

import no.idporten.eudiw.statuslist.repository.models.StatusListDto;
import no.idporten.eudiw.statuslist.service.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("junit")
@DisplayName("MariaDbIntegrationTest with H2 -")
public class MariaDbIntegrationTest {
    @Autowired
    private StatusListProperties properties;

    @Autowired
    private MariaDbIntegration dbIntegration;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM status_list_entry");
        jdbc.update("DELETE FROM status_list");
        jdbc.execute("ALTER TABLE status_list ALTER COLUMN id RESTART WITH 1");
    }

    @Test
    @DisplayName("Should get the next free statuslist")
    void shouldGetNextFreeList() {
        dbIntegration.createNewStatusList();
        dbIntegration.createNewStatusList();


        StatusListDto sl1 = dbIntegration.getNextFreeStatusListDtoForUpdate();
        assertEquals(1, sl1.id());

        jdbc.update("UPDATE status_list SET next_index = list_size WHERE id = ?", 1);

        StatusListDto sl2 = dbIntegration.getNextFreeStatusListDtoForUpdate();
        assertEquals(2, sl2.id());
    }

    @Test
    @DisplayName("Should create new status_list with correct properties")
    void shouldCreateNewStatusList() {
        StatusListDto sl1 = dbIntegration.createNewStatusList();

        assertEquals(1, sl1.id());
        assertEquals(properties.listSize(), sl1.size());
    }

    @Test
    @DisplayName("Should create new status_list_entry")
    void shouldCreateNewStatusListEntry() {
        StatusListDto sl1 = dbIntegration.createNewStatusList();
        dbIntegration.createStatusListEntry(sl1.id(), sl1.next(), Status.INVALID);

        List<Integer> status = jdbc.query("SELECT (status_value) FROM status_list_entry WHERE status_list_id=?", (rs, i) -> rs.getInt(1),  sl1.id());
        assertEquals(Status.INVALID, status.getFirst());
    }
}

