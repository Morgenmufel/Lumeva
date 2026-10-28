package lumeva.profileservice.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Напрямую в Kafka сервис не пишет: события уходят через outbox и Debezium (ADR-006).
 * Топик событий профиля создаёт этот сервис как его владелец.
 */
@Configuration
public class KafkaConfig {

    /**
     * Число партиций — потолок параллелизма потребителей: 3 партиции = максимум 3 экземпляра
     * одной consumer group читают одновременно. Уменьшить нельзя, увеличение меняет партицию
     * части ключей. Для prod вынесем в конфиг.
     */
    @Bean
    public NewTopic profileEventsTopic(@Value("${lumeva.kafka.topics.profile-events}") String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
