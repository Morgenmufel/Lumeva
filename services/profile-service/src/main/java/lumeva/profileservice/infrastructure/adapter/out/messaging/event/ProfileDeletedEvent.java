package lumeva.profileservice.infrastructure.adapter.out.messaging.event;

import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

public record ProfileDeletedEvent(UUID eventId,
                                  Instant occurredAt,
                                  UUID userId,
                                  String email) {
    @Builder
    public ProfileDeletedEvent {}
}
