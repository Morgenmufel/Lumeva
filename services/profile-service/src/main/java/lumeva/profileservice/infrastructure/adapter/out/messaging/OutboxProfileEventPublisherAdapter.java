package lumeva.profileservice.infrastructure.adapter.out.messaging;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.RequiredArgsConstructor;
import lumeva.events.common.EventMetadata;
import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.domain.port.out.ProfileEventPublisherPort;
import lumeva.profileservice.infrastructure.adapter.out.messaging.mapper.ProfileAvroMapper;
import lumeva.profileservice.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import lumeva.profileservice.infrastructure.adapter.out.persistence.repository.OutboxEventRepository;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Пишет событие в outbox в той же транзакции, что и изменение профиля (ADR-006).
 * MANDATORY: вызов вне транзакции падает — событие нельзя записать отдельно от данных.
 */
@Component
@RequiredArgsConstructor
public class OutboxProfileEventPublisherAdapter implements ProfileEventPublisherPort {

    /** Debezium строит из него топик: lumeva.${aggregate_type}.events */
    private static final String AGGREGATE_TYPE = "profile";

    private final OutboxEventRepository outboxRepository;
    private final ProfileAvroMapper avroMapper;
    private final KafkaAvroSerializer outboxAvroSerializer;

    /**
     * Из имени топика сериализатор строит subject в Registry. Оно обязано совпадать с топиком,
     * куда Debezium положит сообщение, иначе consumer будет искать схему не в том subject.
     */
    @Value("${lumeva.kafka.topics.profile-events}")
    private String topic;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishProfileCreated(UserProfile profile) {
        var metadata = newMetadata(profile);
        save(profile, metadata, avroMapper.toCreated(profile, metadata));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishProfileUpdated(UserProfile profile) {
        var metadata = newMetadata(profile);
        save(profile, metadata, avroMapper.toUpdated(profile, metadata));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishProfileDeleted(UserProfile profile) {
        var metadata = newMetadata(profile);
        save(profile, metadata, avroMapper.toDeleted(profile, metadata));
    }

    private EventMetadata newMetadata(UserProfile profile) {
        return EventMetadata.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(Instant.now())
                .setAggregateVersion(profile.getVersion())
                .build();
    }

    private void save(UserProfile profile, EventMetadata metadata, SpecificRecord event) {
        outboxRepository.save(OutboxEventEntity.builder()
                .id(UUID.fromString(metadata.getEventId()))
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(profile.getId().toString())
                .type(event.getSchema().getName())
                .payload(outboxAvroSerializer.serialize(topic, event))
                .createdAt(metadata.getOccurredAt())
                .build());
    }
}
