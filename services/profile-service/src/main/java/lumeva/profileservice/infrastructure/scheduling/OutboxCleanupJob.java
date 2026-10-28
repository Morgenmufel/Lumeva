package lumeva.profileservice.infrastructure.scheduling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lumeva.profileservice.infrastructure.adapter.out.persistence.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Удаляет из outbox строки старше срока хранения (ADR-006).
 *
 * Доставке это не мешает: Debezium читает WAL через replication slot, а не таблицу,
 * а удаления он пропускает (skipped.operations=u,d,t). Строки держим неделю только для отладки;
 * история событий — в топике Kafka, аудит изменений — в profile_audit_logs.
 *
 * Несколько экземпляров сервиса запустят очистку одновременно — это безопасно: удаление идемпотентно.
 * Если объём вырастет до миллионов строк в сутки, перейду на партиции по дням (DROP PARTITION вместо DELETE).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupJob {

    private final OutboxEventRepository outboxRepository;

    @Value("${lumeva.outbox.retention}")
    private Duration retention;

    @Scheduled(cron = "${lumeva.outbox.cleanup-cron}", zone = "UTC")
    @Transactional
    public void cleanUp() {
        Instant threshold = Instant.now().minus(retention);
        int deleted = outboxRepository.deleteCreatedBefore(threshold);
        log.info("Outbox cleanup: deleted {} events created before {}", deleted, threshold);
    }
}
