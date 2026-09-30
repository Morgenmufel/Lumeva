package lumeva.profileservice.infrastructure.adapter.out.cache.mapper;


import lumeva.profileservice.domain.model.*;
import lumeva.profileservice.infrastructure.adapter.out.cache.dto.ProfileCacheDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProfileCacheMapper {

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
    ProfileCacheDto toCacheDto(UserProfile domain);

    default UserProfile toDomain(ProfileCacheDto dto) {
        if (dto == null) return null;

        PersonalInfo personalInfo = new PersonalInfo(
                dto.getFirstName(), dto.getLastName(), dto.getDisplayName(), dto.getBio(), dto.getSocialLinks()
        );

        Preferences preferences = new Preferences(
                dto.getTheme() != null ? dto.getTheme() : "dark",
                dto.getLocale() != null ? dto.getLocale() : "ru",
                dto.getTimezone() != null ? dto.getTimezone() : "UTC",
                dto.isEmailNotifications(),
                dto.isPushNotifications()
        );

        Avatar avatar = new Avatar(
                dto.getOriginalAvatarUrl(), dto.getThumbnailAvatarUrl(), dto.getAvatarUploadedAt()
        );

        return new UserProfile(
                dto.getId(),
                dto.getEmail(),
                personalInfo,
                preferences,
                avatar,
                AdminMetadata.empty(),
                ProfileStatus.valueOf(dto.getStatus()),
                dto.getCreatedAt(),
                dto.getUpdatedAt()
        );
    }
}