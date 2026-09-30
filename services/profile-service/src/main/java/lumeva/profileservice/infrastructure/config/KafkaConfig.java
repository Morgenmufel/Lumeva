package lumeva.profileservice.infrastructure.config;



import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${lumeva.kafka.topics.profile-created}")
    private String topicCreated;


    @Value("${lumeva.kafka.topics.profile-updated}")
    private String topicUpdated;

    @Value("${lumeva.kafka.topics.profile-banned}")
    private String topicBanned;

    @Value("${lumeva.kafka.topics.profile-unbanned}")
    private String topicUnbanned;

    @Value("${lumeva.kafka.topics.profile-deleted}")
    private String topicDeleted;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);

        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    public NewTopic profileUpdatedTopic() {
        return TopicBuilder.name(topicUpdated).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic profileBannedTopic() {
        return TopicBuilder.name(topicBanned).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic profileUnbannedTopic() {
        return TopicBuilder.name(topicUnbanned).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic profileCreatedTopic(){
        return TopicBuilder.name(topicCreated).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic profileDeletedTopic(){
        return TopicBuilder.name(topicDeleted).partitions(3).replicas(1).build();
    }
}