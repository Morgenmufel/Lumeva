package lumeva.profileservice.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Вся инфраструктура profile-service в контейнерах — те же образы, что в infra/docker/compose.yml.
 * @ServiceConnection сам передаёт Spring адреса и пароли контейнеров, url в yml не нужны.
 * Schema Registry не поднимаем: в профиле test стоит mock:// — Registry в памяти из библиотеки Confluent.
 */
public interface ProfileServiceContainers {

    @ServiceConnection
    PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @ServiceConnection
    KafkaContainer KAFKA = new KafkaContainer("apache/kafka:4.0.0");

    @ServiceConnection
    MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0");

    @ServiceConnection(name = "redis")
    GenericContainer<?> VALKEY = new GenericContainer<>("valkey/valkey:8").withExposedPorts(6379);
}
