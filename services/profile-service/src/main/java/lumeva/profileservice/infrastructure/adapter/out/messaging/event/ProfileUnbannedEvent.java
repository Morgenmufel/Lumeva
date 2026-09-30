package lumeva.profileservice.infrastructure.adapter.out.messaging.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

public record ProfileUnbannedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID userId,
        UUID adminId
) {
    @Builder
    public ProfileUnbannedEvent {}
}