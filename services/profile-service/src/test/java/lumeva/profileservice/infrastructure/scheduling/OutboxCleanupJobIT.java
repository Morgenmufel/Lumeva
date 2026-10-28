package lumeva.profileservice.infrastructure.scheduling;

import lumeva.profileservice.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxCleanupJobIT extends AbstractIntegrationTest {

    @Autowired private OutboxCleanupJob cleanupJob;
    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("Очистка удаляет строки старше 7 дней и не трогает свежие")
    void deletesOnlyExpiredRows() {
        UUID expired = insert(Instant.now().minus(Duration.ofDays(8)));
        UUID fresh = insert(Instant.now().minus(Duration.ofDays(6)));

        cleanupJob.cleanUp();

        assertThat(exists(expired)).isFalse();
        assertThat(exists(fresh)).isTrue();
    }

    private UUID insert(Instant createdAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into outbox_events (id, aggregate_type, aggregate_id, type, payload, created_at)"
                        + " values (?, 'profile', ?, 'Test', ?, ?)",
                id, UUID.randomUUID().toString(), new byte[]{0}, Timestamp.from(createdAt));
        return id;
    }

    private boolean exists(UUID id) {
        Integer count = jdbc.queryForObject("select count(*) from outbox_events where id = ?", Integer.class, id);
        return count != null && count > 0;
    }
}
