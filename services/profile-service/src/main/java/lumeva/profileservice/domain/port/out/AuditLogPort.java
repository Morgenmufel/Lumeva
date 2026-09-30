package lumeva.profileservice.domain.port.out;

import java.util.UUID;

public interface AuditLogPort {
    void logAction(UUID targetUserId, UUID actorId, String role, String action, String reason);
}
