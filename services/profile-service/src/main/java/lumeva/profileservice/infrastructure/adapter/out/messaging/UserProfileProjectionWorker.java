package lumeva.profileservice.infrastructure.adapter.out.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lumeva.profileservice.domain.port.out.ProfileCachePort;
import lumeva.profileservice.infrastructure.adapter.out.messaging.event.*;
import lumeva.profileservice.infrastructure.adapter.out.persistence.mongo.UserProfileReadDocument;
import lumeva.profileservice.infrastructure.adapter.out.persistence.repository.SpringDataMongoProfileRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileProjectionWorker {

    private final SpringDataMongoProfileRepository mongoRepository;
    private final ProfileCachePort profileCache;

    @KafkaListener(topics = "${lumeva.kafka.topics.profile-created}", groupId = "profile-projection-group")
    public void onProfileCreated(ProfileCreatedEvent event) {
        log.info("CDC Event: Projecting PROFILE_CREATED to Mongo for user {}", event.userId());

        var doc = mongoRepository.findById(event.userId())
                .orElseGet(() -> UserProfileReadDocument.builder().id(event.userId()).build());

        doc.setEmail(event.email());
        doc.setStatus(event.status());
        doc.setCreatedAt(event.occurredAt());
        doc.setUpdatedAt(event.occurredAt());

        mongoRepository.save(doc);
        profileCache.evict(event.userId());
    }

    @KafkaListener(topics = "${lumeva.kafka.topics.profile-updated}", groupId = "profile-projection-group")
    public void onProfileUpdated(ProfileUpdatedEvent event) {
        log.info("CDC Event: Projecting PROFILE_UPDATED to Mongo for user {}", event.userId());

        var doc = mongoRepository.findById(event.userId())
                .orElseGet(() -> UserProfileReadDocument.builder().id(event.userId()).build());

        doc.setEmail(event.email());
        doc.setStatus(event.status());
        doc.setFirstName(event.firstName());
        doc.setLastName(event.lastName());
        doc.setDisplayName(event.displayName());
        doc.setBio(event.bio());
        doc.setThumbnailAvatarUrl(event.avatarUrl());
        doc.setUpdatedAt(event.occurredAt());

        mongoRepository.save(doc);
        profileCache.evict(event.userId());
    }

    @KafkaListener(topics = "${lumeva.kafka.topics.profile-banned}", groupId = "profile-projection-group")
    public void onProfileBanned(ProfileBannedEvent event) {
        log.info("CDC Event: Projecting PROFILE_BANNED to Mongo for user {}", event.userId());

        mongoRepository.findById(event.userId()).ifPresentOrElse(
                doc -> {
                    doc.setStatus("BANNED");
                    doc.setUpdatedAt(event.occurredAt());
                    mongoRepository.save(doc);
                },
                () -> log.warn("Cannot project PROFILE_BANNED: Document for user {} not found in Mongo", event.userId())
        );

        profileCache.evict(event.userId());
    }

    @KafkaListener(topics = "${lumeva.kafka.topics.profile-unbanned}", groupId = "profile-projection-group")
    public void onProfileUnbanned(ProfileUnbannedEvent event) {
        log.info("CDC Event: Projecting PROFILE_UNBANNED to Mongo for user {}", event.userId());

        mongoRepository.findById(event.userId()).ifPresentOrElse(
                doc -> {
                    doc.setStatus("ACTIVE");
                    doc.setUpdatedAt(event.occurredAt());
                    mongoRepository.save(doc);
                },
                () -> log.warn("Cannot project PROFILE_UNBANNED: Document for user {} not found in Mongo", event.userId())
        );

        profileCache.evict(event.userId());
    }

    @KafkaListener(topics = "${lumeva.kafka.topics.profile-deleted}", groupId = "profile-projection-group")
    public void onProfileDeleted(ProfileDeletedEvent event) {
        log.info("CDC Event: Projecting PROFILE_DELETED to Mongo for user {}", event.userId());

        mongoRepository.findById(event.userId()).ifPresentOrElse(
                doc -> {
                    doc.setStatus("DELETED");
                    doc.setUpdatedAt(event.occurredAt());
                    mongoRepository.save(doc);
                },
                () -> {
                    var doc = UserProfileReadDocument.builder()
                            .id(event.userId())
                            .email(event.email())
                            .status("DELETED")
                            .updatedAt(event.occurredAt())
                            .build();
                    mongoRepository.save(doc);
                }
        );

        profileCache.evict(event.userId());
    }
}