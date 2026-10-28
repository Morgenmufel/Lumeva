package lumeva.profileservice.infrastructure.adapter.out.persistence.mongo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lumeva.events.common.EventMetadata;
import lumeva.events.profile.ProfileSnapshot;
import lumeva.profileservice.domain.port.out.ProfileCachePort;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Применяет события профиля к read-модели в Mongo (CQRS: запись в Postgres, чтение из Mongo).
 *
 * Идемпотентность — через версию агрегата (ADR-007): документ заменяется, только если версия события
 * больше сохранённой. Дубль или опоздавшее событие ничего не меняют. Проверка и запись — одна атомарная
 * операция Mongo, поэтому гонки «прочитал старую версию — записал поверх новой» нет.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileReadModelProjector {

    static final String DELETED_STATUS = "DELETED";

    private final MongoTemplate mongoTemplate;
    private final ProfileCachePort profileCache;

    /** Created и Updated несут полный snapshot: документ заменяется целиком. */
    public void applySnapshot(ProfileSnapshot snapshot, EventMetadata metadata) {
        UUID id = UUID.fromString(snapshot.getUserId());
        var document = UserProfileReadDocument.builder()
                .id(id)
                .version(metadata.getAggregateVersion())
                .username(snapshot.getUsername())
                .status(snapshot.getStatus().name())
                .firstName(snapshot.getFirstName())
                .lastName(snapshot.getLastName())
                .displayName(snapshot.getDisplayName())
                .bio(snapshot.getBio())
                .socialLinks(snapshot.getSocialLinks())
                .theme(snapshot.getTheme())
                .locale(snapshot.getLocale())
                .timezone(snapshot.getTimezone())
                .emailNotifications(snapshot.getEmailNotifications())
                .pushNotifications(snapshot.getPushNotifications())
                .originalAvatarUrl(snapshot.getOriginalAvatarUrl())
                .thumbnailAvatarUrl(snapshot.getThumbnailAvatarUrl())
                .createdAt(snapshot.getCreatedAt())
                .updatedAt(snapshot.getUpdatedAt())
                .build();
        replaceIfNewer(id, metadata, document);
    }

    /**
     * Удаление — не удаление документа, а «надгробие»: id, статус DELETED и версия, без персональных данных.
     * Если документ стереть, опоздавший Updated с меньшей версией не нашёл бы, с чем сравниться,
     * и «воскресил» бы профиль.
     */
    public void applyDeletion(String userId, EventMetadata metadata) {
        UUID id = UUID.fromString(userId);
        var tombstone = UserProfileReadDocument.builder()
                .id(id)
                .version(metadata.getAggregateVersion())
                .status(DELETED_STATUS)
                .updatedAt(metadata.getOccurredAt())
                .build();
        replaceIfNewer(id, metadata, tombstone);
    }

    private void replaceIfNewer(UUID id, EventMetadata metadata, UserProfileReadDocument document) {
        long version = metadata.getAggregateVersion();
        // Документ с меньшей версией или старый документ без поля version
        Query olderDocument = Query.query(Criteria.where("_id").is(id).orOperator(
                Criteria.where("version").lt(version),
                Criteria.where("version").exists(false)));
        try {
            // upsert: документа нет — вставится. Документ есть, но его версия >= — запрос его не найдёт,
            // Mongo попробует вставить второй документ с тем же _id и получит DuplicateKey. Это и значит «устарело».
            mongoTemplate.findAndReplace(olderDocument, document, FindAndReplaceOptions.options().upsert());
            profileCache.evict(id);
            log.debug("Projected event {} for profile {} at version {}", metadata.getEventId(), id, version);
        } catch (DuplicateKeyException stale) {
            log.debug("Skipped stale or duplicate event {} for profile {} at version {}",
                    metadata.getEventId(), id, version);
        }
    }
}
