package lumeva.profileservice.infrastructure.adapter.out.messaging;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lumeva.events.profile.ProfileCreated;
import lumeva.events.profile.ProfileDeleted;
import lumeva.events.profile.ProfileUpdated;
import lumeva.profileservice.application.dto.DeleteProfileCommand;
import lumeva.profileservice.application.dto.ProvisionProfileCommand;
import lumeva.profileservice.application.dto.UpdatePreferencesCommand;
import lumeva.profileservice.application.service.UserProfileApplicationService;
import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.domain.port.out.ProfileEventPublisherPort;
import lumeva.profileservice.support.AbstractIntegrationTest;
import lumeva.profileservice.support.OutboxTestSupport.OutboxRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;

import java.util.List;
import java.util.UUID;

import static lumeva.profileservice.support.OutboxTestSupport.rowsFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Producer-сторона outbox на настоящем Postgres: какие строки увидит Debezium после операций с профилем. */
class OutboxPublishingIT extends AbstractIntegrationTest {

    @Autowired private UserProfileApplicationService profileService;
    @Autowired private ProfileEventPublisherPort eventPublisher;
    @Autowired private KafkaAvroDeserializer profileEventsAvroDeserializer;
    @Autowired private JdbcTemplate jdbc;

    @Value("${lumeva.kafka.topics.profile-events}")
    private String topic;

    @Test
    @DisplayName("Создание, изменение и удаление пишут в outbox Avro-события с растущей версией")
    void lifecycleWritesAvroEventsWithIncreasingVersion() {
        UUID userId = UUID.randomUUID();

        profileService.createInitialProfile(new ProvisionProfileCommand(userId, username(userId)));
        profileService.updatePreferences(
                new UpdatePreferencesCommand(userId, "light", "en", "Europe/Moscow", false, true));
        profileService.deleteProfile(DeleteProfileCommand.of(userId));

        List<OutboxRow> rows = rowsFor(jdbc, userId);
        assertThat(rows).extracting(OutboxRow::type)
                .containsExactly("ProfileCreated", "ProfileUpdated", "ProfileDeleted");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.aggregateType()).isEqualTo("profile");
            assertThat(row.aggregateId()).isEqualTo(userId.toString());
        });

        var created = (ProfileCreated) deserialize(rows.get(0));
        var updated = (ProfileUpdated) deserialize(rows.get(1));
        var deleted = (ProfileDeleted) deserialize(rows.get(2));

        // Регрессия на saveAndFlush: без flush второе событие ушло бы с версией 0
        assertThat(created.getMetadata().getAggregateVersion()).isZero();
        assertThat(updated.getMetadata().getAggregateVersion()).isEqualTo(1L);
        assertThat(deleted.getMetadata().getAggregateVersion()).isEqualTo(2L);

        assertThat(updated.getProfile().getTheme()).isEqualTo("light");
        assertThat(updated.getProfile().getTimezone()).isEqualTo("Europe/Moscow");
        assertThat(deleted.getUserId()).isEqualTo(userId.toString());

        // id строки = eventId: один идентификатор в заголовке Kafka, логах и DLT
        assertThat(rows.get(0).id()).isEqualTo(UUID.fromString(created.getMetadata().getEventId()));
    }

    @Test
    @DisplayName("Повторное создание профиля не пишет второе событие (идемпотентный provisioning)")
    void repeatedProvisioningDoesNotDuplicateEvent() {
        UUID userId = UUID.randomUUID();
        var command = new ProvisionProfileCommand(userId, username(userId));

        profileService.createInitialProfile(command);
        profileService.createInitialProfile(command);

        assertThat(rowsFor(jdbc, userId)).hasSize(1);
    }

    @Test
    @DisplayName("Publisher вне транзакции падает: событие нельзя записать отдельно от данных")
    void publisherRequiresTransaction() {
        UserProfile profile = UserProfile.createInitial(UUID.randomUUID(), "no_tx");

        assertThatThrownBy(() -> eventPublisher.publishProfileCreated(profile))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private Object deserialize(OutboxRow row) {
        return profileEventsAvroDeserializer.deserialize(topic, row.payload());
    }

    private static String username(UUID userId) {
        return "it_" + userId.toString().substring(0, 8);
    }
}
