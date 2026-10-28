package lumeva.profileservice.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

/** Читает строки outbox так, как их видит Debezium: колонки таблицы, без JPA. */
public final class OutboxTestSupport {

    public record OutboxRow(UUID id, String aggregateType, String aggregateId, String type, byte[] payload) {
    }

    private OutboxTestSupport() {
    }

    public static List<OutboxRow> rowsFor(JdbcTemplate jdbc, UUID aggregateId) {
        return jdbc.query(
                "select id, aggregate_type, aggregate_id, type, payload from outbox_events"
                        + " where aggregate_id = ? order by created_at",
                (rs, rowNum) -> new OutboxRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("aggregate_type"),
                        rs.getString("aggregate_id"),
                        rs.getString("type"),
                        rs.getBytes("payload")),
                aggregateId.toString());
    }
}
