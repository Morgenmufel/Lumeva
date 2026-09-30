package lumeva.profileservice.infrastructure.adapter.out.messaging.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

public record ProfileUpdatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String displayName,
        String bio,
        String avatarUrl,
        String status
) {
    @Builder
    public ProfileUpdatedEvent {}
}