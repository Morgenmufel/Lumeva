package lumeva.profileservice.infrastructure.adapter.out.messaging;

import lumeva.profileservice.domain.port.out.ProfileCachePort;
import lumeva.profileservice.infrastructure.adapter.out.messaging.event.ProfileCreatedEvent;
import lumeva.profileservice.infrastructure.adapter.out.messaging.event.ProfileUpdatedEvent;
import lumeva.profileservice.infrastructure.adapter.out.persistence.mongo.UserProfileReadDocument;
import lumeva.profileservice.infrastructure.adapter.out.persistence.repository.SpringDataMongoProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Testcontainers
class UserProfileProjectionWorkerTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private UserProfileProjectionWorker projectionWorker;

    @Autowired
    private SpringDataMongoProfileRepository mongoRepository;

    @Mock
    private ProfileCachePort profileCache;

    @BeforeEach
    void setUp() {
        mongoRepository.deleteAll();
    }

    @Test
    @DisplayName("onProfileCreated должен создавать новый документ в MongoDB и инвалидировать кэш")
    void shouldProjectProfileCreatedToMongo() {
        UUID userId = UUID.randomUUID();
        ProfileCreatedEvent event = ProfileCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .email("mongo@lumeva.com")
                .status("ACTIVE")
                .occurredAt(Instant.now())
                .build();

        projectionWorker.onProfileCreated(event);

        Optional<UserProfileReadDocument> docOpt = mongoRepository.findById(userId);
        assertThat(docOpt).isPresent();
        assertThat(docOpt.get().getEmail()).isEqualTo("mongo@lumeva.com");
        assertThat(docOpt.get().getStatus()).isEqualTo("ACTIVE");

        verify(profileCache).evict(userId);
    }

    @Test
    @DisplayName("onProfileUpdated должен обновлять имеющийся документ в Mongo")
    void shouldProjectProfileUpdatedToMongo() {
        UUID userId = UUID.randomUUID();
        mongoRepository.save(UserProfileReadDocument.builder()
                .id(userId)
                .email("old@lumeva.com")
                .status("ACTIVE")
                .build());

        ProfileUpdatedEvent updateEvent = ProfileUpdatedEvent.builder()
                .eventId(UUID.randomUUID())
                .userId(userId)
                .email("old@lumeva.com")
                .status("ACTIVE")
                .firstName("UpdatedName")
                .lastName("UpdatedLast")
                .displayName("updated_nick")
                .occurredAt(Instant.now())
                .build();

        projectionWorker.onProfileUpdated(updateEvent);

        UserProfileReadDocument updatedDoc = mongoRepository.findById(userId).orElseThrow();
        assertThat(updatedDoc.getFirstName()).isEqualTo("UpdatedName");
        assertThat(updatedDoc.getDisplayName()).isEqualTo("updated_nick");
        verify(profileCache).evict(userId);
    }
}