package lumeva.profileservice.infrastructure.adapter.in.messaging;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lumeva.events.profile.ProfileCreated;
import lumeva.events.profile.ProfileDeleted;
import lumeva.events.profile.ProfileUpdated;
import lumeva.profileservice.infrastructure.adapter.out.persistence.mongo.ProfileReadModelProjector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Читает lumeva.profile.events и строит read-модель профиля.
 *
 * Значение приходит байтами, Avro разбираем здесь, а не в десериализаторе Kafka:
 * если разбор упадёт, в DLT уйдут исходные байты, и после исправления их можно переиграть как есть.
 * Ошибки обрабатывает DefaultErrorHandler (KafkaConsumerConfig): временные повторяются, остальные — в DLT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileEventsListener {

    /** Заголовок с типом события; его проставляет Debezium из колонки type таблицы outbox. */
    static final String EVENT_TYPE_HEADER = "eventType";

    private static final Set<String> SUPPORTED_EVENT_TYPES = Set.of(
            ProfileCreated.getClassSchema().getName(),
            ProfileUpdated.getClassSchema().getName(),
            ProfileDeleted.getClassSchema().getName());

    private final KafkaAvroDeserializer profileEventsAvroDeserializer;
    private final ProfileReadModelProjector projector;

    @KafkaListener(
            topics = "${lumeva.kafka.topics.profile-events}",
            groupId = "${lumeva.kafka.projection.group-id}")
    public void onEvent(ConsumerRecord<String, byte[]> record) {
        String eventType = header(record, EVENT_TYPE_HEADER);
        if (eventType != null && !SUPPORTED_EVENT_TYPES.contains(eventType)) {
            // Tolerant reader: producer добавил новый тип события, проекции он не нужен.
            // Пропускаем, а не падаем: иначе каждое новое событие в контракте ломало бы старых потребителей.
            log.debug("Skipping unsupported event type {} at {}-{}@{}",
                    eventType, record.topic(), record.partition(), record.offset());
            return;
        }

        Object event = profileEventsAvroDeserializer.deserialize(record.topic(), record.value());
        switch (event) {
            case ProfileCreated created -> projector.applySnapshot(created.getProfile(), created.getMetadata());
            case ProfileUpdated updated -> projector.applySnapshot(updated.getProfile(), updated.getMetadata());
            case ProfileDeleted deleted -> projector.applyDeletion(deleted.getUserId(), deleted.getMetadata());
            case null -> throw new IllegalArgumentException("Empty payload at %s-%d@%d"
                    .formatted(record.topic(), record.partition(), record.offset()));
            default -> log.warn("Skipping unexpected event class {} at {}-{}@{}",
                    event.getClass().getName(), record.topic(), record.partition(), record.offset());
        }
    }

    private static String header(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null || header.value() == null
                ? null
                : new String(header.value(), StandardCharsets.UTF_8);
    }
}
