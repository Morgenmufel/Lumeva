package lumeva.profileservice.infrastructure.adapter.out.persistence.repository;

import lumeva.profileservice.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT 100", nativeQuery = true)
    List<OutboxEventEntity> findPendingEventsBatch();
}
