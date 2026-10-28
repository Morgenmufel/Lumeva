package lumeva.profileservice.infrastructure.adapter.in.messaging;

import lumeva.profileservice.application.dto.DeleteProfileCommand;
import lumeva.profileservice.application.dto.ProvisionProfileCommand;
import lumeva.profileservice.application.dto.UpdatePreferencesCommand;
import lumeva.profileservice.application.service.UserProfileApplicationService;
import lumeva.profileservice.domain.port.out.UserProfileReadRepositoryPort;
import lumeva.profileservice.infrastructure.adapter.out.persistence.mongo.UserProfileReadDocument;
import lumeva.profileservice.support.AbstractIntegrationTest;
import lumeva.profileservice.support.OutboxTestSupport.OutboxRow;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static lumeva.profileservice.support.OutboxTestSupport.rowsFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Consumer-сторона на настоящих Kafka и Mongo. Debezium в тесте не поднимаю: тест делает его работу сам —
 * берёт строку outbox и публикует payload с тем же ключом и заголовками. Сам Debezium
 * проверяется вручную через compose (infra/docker/README.md).
 */
class ProfileProjectionIT extends AbstractIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Autowired private UserProfileApplicationService profileService;
    @Autowired private UserProfileReadRepositoryPort readRepository;
    @Autowired private KafkaTemplate<String, byte[]> kafkaTemplate;
    @Autowired private ConsumerFactory<String, byte[]> consumerFactory;
    @Autowired private KafkaAdmin kafkaAdmin;
    @Autowired private MongoTemplate mongoTemplate;
    @Autowired private JdbcTemplate jdbc;

    @Value("${lumeva.kafka.topics.profile-events}")
    private String topic;

    @Value("${lumeva.kafka.topics.profile-events-projection-dlt}")
    private String dltTopic;

    @Value("${lumeva.kafka.projection.group-id}")
    private String projectionGroupId;

    @Test
    @DisplayName("Проекция применяет события по версии: дубль и опоздавшее событие ничего не меняют, удаление — надгробие")
    void projectionIsIdempotentAndOrderSafe() throws Exception {
        UUID userId = UUID.randomUUID();
        profileService.createInitialProfile(
                new ProvisionProfileCommand(userId, "proj_" + userId.toString().substring(0, 8)));
        profileService.updatePreferences(new UpdatePreferencesCommand(userId, "light", "en", "UTC", true, false));
        List<OutboxRow> rows = rowsFor(jdbc, userId);
        OutboxRow created = rows.get(0);
        OutboxRow updated = rows.get(1);

        relay(created);
        relay(updated);
        await().atMost(TIMEOUT).untilAsserted(() -> {
            var document = document(userId);
            assertThat(document).isNotNull();
            assertThat(document.getVersion()).isEqualTo(1L);
            assertThat(document.getTheme()).isEqualTo("light");
        });

        // Повторная доставка старого события (at-least-once): документ не откатывается
        awaitConsumed(relay(created));
        assertThat(document(userId).getVersion()).isEqualTo(1L);
        assertThat(document(userId).getTheme()).isEqualTo("light");

        profileService.deleteProfile(DeleteProfileCommand.of(userId));
        relay(rowsFor(jdbc, userId).get(2));
        await().atMost(TIMEOUT).untilAsserted(() -> {
            var tombstone = document(userId);
            assertThat(tombstone.getStatus()).isEqualTo("DELETED");
            assertThat(tombstone.getVersion()).isEqualTo(2L);
            assertThat(tombstone.getUsername()).isNull();
        });
        assertThat(readRepository.findPublicProfile(userId)).isEmpty();

        // Опоздавший Updated после удаления не «воскрешает» профиль
        awaitConsumed(relay(updated));
        assertThat(document(userId).getStatus()).isEqualTo("DELETED");
    }

    @Test
    @DisplayName("Сообщение, которое не разбирается, сразу уходит в DLT с исходными байтами")
    void poisonMessageGoesToDltAsIs() throws Exception {
        String key = UUID.randomUUID().toString();
        byte[] poison = "not avro".getBytes(StandardCharsets.UTF_8);

        var record = new ProducerRecord<>(topic, key, poison);
        record.headers().add(new RecordHeader(ProfileEventsListener.EVENT_TYPE_HEADER,
                "ProfileCreated".getBytes(StandardCharsets.UTF_8)));
        kafkaTemplate.send(record).get();

        try (Consumer<String, byte[]> dltConsumer = consumerFactory.createConsumer("it-dlt-reader-" + key, null)) {
            dltConsumer.subscribe(List.of(dltTopic));
            List<ConsumerRecord<String, byte[]>> received = new ArrayList<>();
            await().atMost(TIMEOUT).untilAsserted(() -> {
                dltConsumer.poll(Duration.ofMillis(500)).forEach(received::add);
                assertThat(received).anySatisfy(dead -> {
                    assertThat(dead.key()).isEqualTo(key);
                    assertThat(dead.value()).isEqualTo(poison);
                    // DeadLetterPublishingRecoverer кладёт класс и текст ошибки в заголовки
                    assertThat(dead.headers().lastHeader("kafka_dlt-exception-fqcn")).isNotNull();
                });
            });
        }
    }

    /** То, что в проде делает Debezium EventRouter. */
    private RecordMetadata relay(OutboxRow row) throws Exception {
        var record = new ProducerRecord<>(topic, row.aggregateId(), row.payload());
        record.headers().add(new RecordHeader("id", row.id().toString().getBytes(StandardCharsets.UTF_8)));
        record.headers().add(new RecordHeader(ProfileEventsListener.EVENT_TYPE_HEADER,
                row.type().getBytes(StandardCharsets.UTF_8)));
        return kafkaTemplate.send(record).get().getRecordMetadata();
    }

    /** Ждём, пока consumer group закоммитит offset за этим сообщением, то есть обработает его. */
    private void awaitConsumed(RecordMetadata sent) {
        var partition = new TopicPartition(sent.topic(), sent.partition());
        try (Admin admin = Admin.create(kafkaAdmin.getConfigurationProperties())) {
            await().atMost(TIMEOUT).until(() -> {
                Map<TopicPartition, OffsetAndMetadata> offsets = admin
                        .listConsumerGroupOffsets(projectionGroupId)
                        .partitionsToOffsetAndMetadata().get();
                OffsetAndMetadata committed = offsets.get(partition);
                return committed != null && committed.offset() > sent.offset();
            });
        }
    }

    private UserProfileReadDocument document(UUID userId) {
        return mongoTemplate.findById(userId, UserProfileReadDocument.class);
    }
}
