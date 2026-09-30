package lumeva.profileservice.application.event;

import java.util.UUID;

public record ProfileCacheSyncEvent(UUID userId) {
}
