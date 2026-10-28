package lumeva.profileservice.infrastructure.config;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class AvroConfig {

    /**
     * Сериализует события в Avro для outbox. Схема ищется (или регистрируется) в Registry
     * при первом событии каждого типа, дальше id схемы берётся из кэша.
     * TopicRecordNameStrategy: у каждого типа события свой subject "<topic>-<полное имя записи>" (Р2).
     */
    @Bean(destroyMethod = "close")
    public KafkaAvroSerializer outboxAvroSerializer(
            @Value("${lumeva.kafka.schema-registry-url}") String schemaRegistryUrl,
            @Value("${lumeva.kafka.auto-register-schemas}") boolean autoRegisterSchemas) {
        var serializer = new KafkaAvroSerializer();
        serializer.configure(Map.of(
                "schema.registry.url", schemaRegistryUrl,
                "auto.register.schemas", autoRegisterSchemas,
                "value.subject.name.strategy", TopicRecordNameStrategy.class.getName()
        ), false); // false: сериализуем value, а не key
        return serializer;
    }

    /**
     * Разбирает события из Kafka в сгенерированные классы (specific.avro.reader).
     * Схему писателя берёт из Registry по id в начале сообщения; несовместимые изменения
     * Registry не пропустит ещё при регистрации, поэтому старый код читает новые версии схемы.
     */
    @Bean(destroyMethod = "close")
    public KafkaAvroDeserializer profileEventsAvroDeserializer(
            @Value("${lumeva.kafka.schema-registry-url}") String schemaRegistryUrl) {
        var deserializer = new KafkaAvroDeserializer();
        deserializer.configure(Map.of(
                "schema.registry.url", schemaRegistryUrl,
                "specific.avro.reader", true,
                "value.subject.name.strategy", TopicRecordNameStrategy.class.getName()
        ), false);
        return deserializer;
    }
}
