package lumeva.profileservice.infrastructure.adapter.out.messaging;


import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.domain.port.out.ProfileEventPublisherPort;
import lumeva.profileservice.infrastructure.adapter.out.messaging.mapper.ProfileEventMapper;
import lumeva.profileservice.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import lumeva.profileservice.infrastructure.adapter.out.persistence.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxProfileEventPublisherAdapter implements ProfileEventPublisherPort {

    private final OutboxEventRepository outboxRepository;
    private final ProfileEventMapper eventMapper;
    private final JsonMapper jsonMapper;

    @Override
    public void publishProfileCreated(UserProfile profile) {
        var event = eventMapper.toCreatedEvent(profile);
        saveToOutbox(profile.getId().toString(), "PROFILE_CREATED", event);
    }

    @Override
    public void publishProfileUpdated(UserProfile profile) {
        var event = eventMapper.toUpdatedEvent(profile);
        saveToOutbox(profile.getId().toString(), "PROFILE_UPDATED", event);
    }

    @Override
    public void publishProfileBanned(UUID userId, String reason) {
        var event = eventMapper.toBannedEvent(userId, reason);
        saveToOutbox(userId.toString(), "PROFILE_BANNED", event);
    }

    @Override
    public void publishProfileUnbanned(UUID userId) {
        var event = eventMapper.toUnbannedEvent(userId);
        saveToOutbox(userId.toString(), "PROFILE_UNBANNED", event);
    }

    @Override
    public void publishProfileDeleted(UUID userId, String email) {
        var event = eventMapper.toDeletedEvent(userId, email);
        saveToOutbox(userId.toString(), "PROFILE_DELETED", event);
    }

    private void saveToOutbox(String aggregateId, String eventType, Object payload) {
        try {
            String jsonPayload = jsonMapper.writeValueAsString(payload);
            var outboxEntity = OutboxEventEntity.builder()
                    .id(UUID.randomUUID())
                    .aggregateType("USER_PROFILE")
                    .aggregateId(aggregateId)
                    .type(eventType)
                    .payload(jsonPayload)
                    .createdAt(Instant.now())
                    .build();

            outboxRepository.save(outboxEntity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}
