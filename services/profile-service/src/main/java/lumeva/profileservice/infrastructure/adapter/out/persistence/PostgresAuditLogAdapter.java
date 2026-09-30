package lumeva.profileservice.infrastructure.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lumeva.profileservice.domain.port.out.AuditLogPort;
import lumeva.profileservice.infrastructure.adapter.out.persistence.entity.AuditLogEntity;
import lumeva.profileservice.infrastructure.adapter.out.persistence.repository.SpringDataAuditLogRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostgresAuditLogAdapter implements AuditLogPort {

    private final SpringDataAuditLogRepository auditLogRepository;

    @Override
    public void logAction(UUID targetUserId, UUID actorId, String role, String action, String reason) {
        AuditLogEntity entity = AuditLogEntity.builder()
                .id(UUID.randomUUID())
                .targetUserId(targetUserId)
                .actorId(actorId)
                .actorRole(role)
                .action(action)
                .reason(reason)
                .createdAt(Instant.now())
                .build();

        auditLogRepository.save(entity);
    }
}