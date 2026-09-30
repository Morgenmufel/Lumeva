package lumeva.profileservice.infrastructure.adapter.out.messaging.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

public record ProfileCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID userId,
        String email,
        String status
) {
    @Builder
    public ProfileCreatedEvent {}
}
