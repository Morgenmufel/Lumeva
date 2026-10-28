package lumeva.profileservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Строка outbox: событие, записанное в одной транзакции с изменением профиля.
 * В Kafka его доставляет Debezium EventRouter (ADR-006); имена колонок заданы в конфиге коннектора.
 * После вставки строка не меняется: сеттеров нет.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OutboxEventEntity {

    /** Совпадает с eventId в метаданных события; Debezium кладёт его в заголовок id. */
    @Id
    private UUID id;

    /** Из него Debezium строит топик: lumeva.${aggregate_type}.events */
    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    /** Ключ сообщения: все события одного профиля попадают в одну партицию. */
    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(name = "type", nullable = false, length = 128)
    private String type;

    /** Событие в Avro в формате Confluent (magic byte + id схемы + данные). */
    @Column(name = "payload", nullable = false)
    private byte[] payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
