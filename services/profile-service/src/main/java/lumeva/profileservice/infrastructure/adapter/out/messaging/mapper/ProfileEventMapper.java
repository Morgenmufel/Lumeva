package lumeva.profileservice.infrastructure.adapter.out.messaging.mapper;

import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.infrastructure.adapter.out.messaging.event.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = {Instant.class, UUID.class})
public interface ProfileEventMapper {

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "occurredAt", expression = "java(Instant.now())")
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "status", expression = "java(domain.getStatus().name())")
    ProfileCreatedEvent toCreatedEvent(UserProfile domain);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "occurredAt", expression = "java(Instant.now())")
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "firstName", source = "personalInfo.firstName")
    @Mapping(target = "lastName", source = "personalInfo.lastName")
    @Mapping(target = "displayName", source = "personalInfo.displayName")
    @Mapping(target = "bio", source = "personalInfo.bio")
    @Mapping(target = "avatarUrl", source = "avatar.thumbnailUrl")
    @Mapping(target = "status", expression = "java(domain.getStatus().name())")
    ProfileUpdatedEvent toUpdatedEvent(UserProfile domain);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "occurredAt", expression = "java(Instant.now())")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "reason", source = "reason")
    @Mapping(target = "adminId", ignore = true)
    ProfileBannedEvent toBannedEvent(UUID userId, String reason);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "occurredAt", expression = "java(Instant.now())")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "adminId", ignore = true)
    ProfileUnbannedEvent toUnbannedEvent(UUID userId);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID())")
    @Mapping(target = "occurredAt", expression = "java(Instant.now())")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "email", source = "email")
    ProfileDeletedEvent toDeletedEvent(UUID userId, String email);
}
