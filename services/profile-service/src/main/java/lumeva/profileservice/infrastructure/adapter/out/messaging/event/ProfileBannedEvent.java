package lumeva.profileservice.infrastructure.adapter.out.messaging.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

public record ProfileBannedEvent(
        UUID eventId,
        Instant occurredAt,
        UUID userId,
        UUID adminId,
        String reason
) {
    @Builder
    public ProfileBannedEvent {}
}
