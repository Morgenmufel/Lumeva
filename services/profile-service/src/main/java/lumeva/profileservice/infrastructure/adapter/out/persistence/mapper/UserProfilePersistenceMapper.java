package lumeva.profileservice.infrastructure.adapter.out.persistence.mapper;


import lumeva.profileservice.domain.model.*;
import lumeva.profileservice.infrastructure.adapter.out.persistence.entity.UserProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserProfilePersistenceMapper {

    @Mapping(target = "profileStatus", source = "status")
    UserProfile toDomain(UserProfileEntity entity);

    @Mapping(target = "status", source = "profileStatus")
    UserProfileEntity toEntity(UserProfile domain);

    UserProfileEntity.PersonalInfoJson mapPersonalInfo(PersonalInfo personalInfo);
    PersonalInfo mapPersonalInfoJson(UserProfileEntity.PersonalInfoJson json);

    UserProfileEntity.PreferencesJson mapPreferences(Preferences preferences);
    Preferences mapPreferencesJson(UserProfileEntity.PreferencesJson json);

    UserProfileEntity.AvatarJson mapAvatar(Avatar avatar);
    Avatar mapAvatarJson(UserProfileEntity.AvatarJson json);

    UserProfileEntity.AdminMetadataJson mapAdminMetadata(AdminMetadata adminMetadata);
    AdminMetadata mapAdminMetadataJson(UserProfileEntity.AdminMetadataJson json);
}