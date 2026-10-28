package lumeva.profileservice.infrastructure.adapter.in.messaging;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lumeva.events.common.EventMetadata;
import lumeva.events.profile.ProfileCreated;
import lumeva.events.profile.ProfileDeleted;
import lumeva.events.profile.ProfileSnapshot;
import lumeva.events.profile.ProfileStatus;
import lumeva.profileservice.infrastructure.adapter.out.persistence.mongo.ProfileReadModelProjector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тест маршрутизации событий. Проекцию в настоящий Mongo и DLT проверит интеграционный тест (шаг 3.7).
 */
@ExtendWith(MockitoExtension.class)
class ProfileEventsListenerTest {

    private static final String TOPIC = "lumeva.profile.events";
    private static final byte[] PAYLOAD = {0, 0, 0, 0, 1};

    @Mock private KafkaAvroDeserializer deserializer;
    @Mock private ProfileReadModelProjector projector;

    @InjectMocks private ProfileEventsListener listener;

    @Test
    @DisplayName("ProfileCreated применяется как snapshot")
    void createdEventAppliesSnapshot() {
        ProfileCreated event = ProfileCreated.newBuilder()
                .setMetadata(metadata(0))
                .setProfile(snapshot())
                .build();
        when(deserializer.deserialize(TOPIC, PAYLOAD)).thenReturn(event);

        listener.onEvent(record("ProfileCreated"));

        verify(projector).applySnapshot(event.getProfile(), event.getMetadata());
    }

    @Test
    @DisplayName("ProfileDeleted превращается в надгробие")
    void deletedEventAppliesDeletion() {
        String userId = UUID.randomUUID().toString();
        ProfileDeleted event = ProfileDeleted.newBuilder()
                .setMetadata(metadata(3))
                .setUserId(userId)
                .build();
        when(deserializer.deserialize(TOPIC, PAYLOAD)).thenReturn(event);

        listener.onEvent(record("ProfileDeleted"));

        verify(projector).applyDeletion(userId, event.getMetadata());
    }

    @Test
    @DisplayName("Неизвестный тип события пропускается без разбора (tolerant reader)")
    void unknownEventTypeIsSkipped() {
        listener.onEvent(record("ProfileAvatarChanged"));

        verifyNoInteractions(deserializer, projector);
    }

    @Test
    @DisplayName("Пустой payload — ошибка, сообщение уйдёт в DLT")
    void emptyPayloadFails() {
        when(deserializer.deserialize(any(), any())).thenReturn(null);

        assertThatThrownBy(() -> listener.onEvent(record("ProfileCreated")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(projector);
    }

    private static ConsumerRecord<String, byte[]> record(String eventType) {
        var record = new ConsumerRecord<>(TOPIC, 0, 42L, UUID.randomUUID().toString(), PAYLOAD);
        record.headers().add(new RecordHeader(ProfileEventsListener.EVENT_TYPE_HEADER,
                eventType.getBytes(StandardCharsets.UTF_8)));
        return record;
    }

    private static EventMetadata metadata(long version) {
        return EventMetadata.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(Instant.now())
                .setAggregateVersion(version)
                .build();
    }

    private static ProfileSnapshot snapshot() {
        Instant now = Instant.now();
        return ProfileSnapshot.newBuilder()
                .setUserId(UUID.randomUUID().toString())
                .setUsername("user")
                .setStatus(ProfileStatus.ACTIVE)
                .setSocialLinks(Map.of())
                .setTheme("dark")
                .setLocale("ru")
                .setTimezone("UTC")
                .setEmailNotifications(true)
                .setPushNotifications(true)
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .build();
    }
}
