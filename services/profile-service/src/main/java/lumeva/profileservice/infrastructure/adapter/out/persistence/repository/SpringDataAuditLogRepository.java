package lumeva.profileservice.infrastructure.adapter.out.persistence.repository;

import lumeva.profileservice.infrastructure.adapter.out.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
}
