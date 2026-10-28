package lumeva.profileservice.infrastructure.adapter.out.persistence.repository;

import lumeva.profileservice.infrastructure.adapter.out.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /** Одним запросом, без загрузки сущностей в память; использует индекс по created_at. */
    @Modifying
    @Query("delete from OutboxEventEntity e where e.createdAt < :threshold")
    int deleteCreatedBefore(@Param("threshold") Instant threshold);
}
