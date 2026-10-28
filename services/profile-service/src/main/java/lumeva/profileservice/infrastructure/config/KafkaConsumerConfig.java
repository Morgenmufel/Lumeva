package lumeva.profileservice.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.time.Duration;

/**
 * Обработка ошибок consumer'ов (ADR-007).
 *
 * Временные ошибки (Mongo/Redis недоступны) — повторяем на месте, блокируя партицию,
 * с паузой 1с -> 2с -> ... -> 60с без лимита попыток. Порядок событий сохраняется,
 * застрявшая партиция видна по consumer lag.
 *
 * Всё остальное (не разбирается Avro, баг, нарушено правило) — сразу в DLT: повтор не поможет.
 * Список повторяемых ошибок — разрешающий (allow-list): неизвестная ошибка уходит в DLT,
 * а не блокирует партицию навсегда.
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${lumeva.kafka.topics.profile-events-projection-dlt}")
    private String projectionDltTopic;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaTemplate) {
        // Партицию сохраняем: в DLT сообщение лежит рядом с соседями того же ключа
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(projectionDltTopic, record.partition()));

        var backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxInterval(60_000L);

        var errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.defaultFalse();
        errorHandler.addRetryableExceptions(
                TransientDataAccessException.class,
                DataAccessResourceFailureException.class,
                RecoverableDataAccessException.class);
        return errorHandler;
    }

    /**
     * Партиций столько же, сколько у основного топика: recoverer кладёт сообщение в ту же партицию.
     * Храним 30 дней — время разобраться с причиной и переиграть.
     */
    @Bean
    public NewTopic profileEventsProjectionDltTopic() {
        return TopicBuilder.name(projectionDltTopic)
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(30).toMillis()))
                .build();
    }
}
