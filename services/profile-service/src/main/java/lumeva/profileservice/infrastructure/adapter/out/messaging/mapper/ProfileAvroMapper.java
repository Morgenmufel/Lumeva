package lumeva.profileservice.infrastructure.adapter.out.messaging.mapper;

import lumeva.events.common.EventMetadata;
import lumeva.events.profile.ProfileCreated;
import lumeva.events.profile.ProfileDeleted;
import lumeva.events.profile.ProfileSnapshot;
import lumeva.events.profile.ProfileUpdated;
import lumeva.profileservice.domain.model.UserProfile;
import org.springframework.stereotype.Component;

/**
 * Домен -> контракт событий (libs/event-contracts). Написан руками, без MapStruct:
 * у Avro свои builder'ы, а статус переводится из доменного enum в enum контракта.
 * build() проверяет обязательные поля — ошибка контракта видна у producer, а не у потребителя.
 */
@Component
public class ProfileAvroMapper {

    public ProfileCreated toCreated(UserProfile profile, EventMetadata metadata) {
        return ProfileCreated.newBuilder()
                .setMetadata(metadata)
                .setProfile(toSnapshot(profile))
                .build();
    }

    public ProfileUpdated toUpdated(UserProfile profile, EventMetadata metadata) {
        return ProfileUpdated.newBuilder()
                .setMetadata(metadata)
                .setProfile(toSnapshot(profile))
                .build();
    }

    /** Без snapshot: удалённый профиль никто не читает, PII в событие не попадает. */
    public ProfileDeleted toDeleted(UserProfile profile, EventMetadata metadata) {
        return ProfileDeleted.newBuilder()
                .setMetadata(metadata)
                .setUserId(profile.getId().toString())
                .build();
    }

    private ProfileSnapshot toSnapshot(UserProfile profile) {
        var info = profile.getPersonalInfo();
        var prefs = profile.getPreferences();
        var avatar = profile.getAvatar();
        return ProfileSnapshot.newBuilder()
                .setUserId(profile.getId().toString())
                .setUsername(profile.getUsername())
                .setStatus(lumeva.events.profile.ProfileStatus.valueOf(profile.getStatus().name()))
                .setFirstName(info.firstName())
                .setLastName(info.lastName())
                .setDisplayName(info.displayName())
                .setBio(info.bio())
                .setSocialLinks(info.socialLinks())
                .setTheme(prefs.theme())
                .setLocale(prefs.locale())
                .setTimezone(prefs.timezone())
                .setEmailNotifications(prefs.emailNotifications())
                .setPushNotifications(prefs.pushNotifications())
                .setOriginalAvatarUrl(avatar == null ? null : avatar.originalUrl())
                .setThumbnailAvatarUrl(avatar == null ? null : avatar.thumbnailUrl())
                .setCreatedAt(profile.getCreatedAt())
                .setUpdatedAt(profile.getUpdatedAt())
                .build();
    }
}
