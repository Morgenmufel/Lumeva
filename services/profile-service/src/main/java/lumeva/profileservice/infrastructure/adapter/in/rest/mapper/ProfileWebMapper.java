package lumeva.profileservice.infrastructure.adapter.in.rest.mapper;

import lumeva.profileservice.domain.model.AdminMetadata;
import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.infrastructure.adapter.in.rest.dto.AdminMetadataResponse;
import lumeva.profileservice.infrastructure.adapter.in.rest.dto.AdminProfileResponse;
import lumeva.profileservice.infrastructure.adapter.in.rest.dto.MyProfileResponse;
import lumeva.profileservice.infrastructure.adapter.in.rest.dto.PublicProfileResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.Instant;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProfileWebMapper {

    @Mapping(target = "status", expression = "java(domain.getStatus().name())")
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
    MyProfileResponse toMyResponse(UserProfile domain);

    @Mapping(target = "status", expression = "java(domain.getStatus().name())")
    @Mapping(target = "firstName", source = "personalInfo.firstName")
    @Mapping(target = "lastName", source = "personalInfo.lastName")
    @Mapping(target = "displayName", source = "personalInfo.displayName")
    @Mapping(target = "bio", source = "personalInfo.bio")
    @Mapping(target = "socialLinks", source = "personalInfo.socialLinks")
    @Mapping(target = "thumbnailAvatarUrl", source = "avatar.thumbnailUrl")
    PublicProfileResponse toPublicResponse(UserProfile domain);

    @Mapping(target = "status", expression = "java(domain.getStatus().name())")
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
    @Mapping(target = "adminMetadata", expression = "java(mapAdminMetadata(domain.getAdminMetadata(), domain.getSuspendedUntil()))")
    AdminProfileResponse toAdminResponse(UserProfile domain);

    AdminMetadataResponse mapAdminMetadata(AdminMetadata domain, Instant suspendedUntil);
}
