package lumeva.profileservice.infrastructure.adapter.out.persistence.mapper;

import lumeva.profileservice.domain.model.*;
import lumeva.profileservice.infrastructure.adapter.out.persistence.mongo.UserProfileReadDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserProfileMongoMapper {

    @Mapping(target = "firstName", source = "personalInfo.firstName")
    @Mapping(target = "lastName", source = "personalInfo.lastName")
    @Mapping(target = "displayName", source = "personalInfo.displayName")
    @Mapping(target = "bio", source = "personalInfo.bio")
    @Mapping(target = "socialLinks", source = "personalInfo.socialLinks")
    @Mapping(target = "theme", source = "preferences.theme")
    @Mapping(target = "locale", source = "preferences.locale")
    @Mapping(target = "timezone", source = "preferences.timezone")
    @Mapping(target = "emailNotifications", source = "preferences.emailNotifications")
    @Mapping(target = "pushNotifications", source = "preferences.pushNotifications")
    @Mapping(target = "originalAvatarUrl", source = "avatar.originalUrl")
    @Mapping(target = "thumbnailAvatarUrl", source = "avatar.thumbnailUrl")
    @Mapping(target = "avatarUploadedAt", source = "avatar.uploadedAt")
    UserProfileReadDocument toDocument(UserProfile domain);

    default UserProfile toDomain(UserProfileReadDocument doc) {
        if (doc == null) return null;

        PersonalInfo personalInfo = new PersonalInfo(
                doc.getFirstName(), doc.getLastName(), doc.getDisplayName(), doc.getBio(), doc.getSocialLinks()
        );
        Preferences preferences = new Preferences(
                doc.getTheme(), doc.getLocale(), doc.getTimezone(), doc.isEmailNotifications(), doc.isPushNotifications()
        );
        Avatar avatar = new Avatar(
                doc.getOriginalAvatarUrl(), doc.getThumbnailAvatarUrl(), doc.getAvatarUploadedAt()
        );

        return new UserProfile(
                doc.getId(),
                doc.getEmail(),
                personalInfo,
                preferences,
                avatar,
                AdminMetadata.empty(),
                ProfileStatus.valueOf(doc.getStatus()),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
