package lumeva.profileservice.infrastructure.adapter.in.rest.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AdminProfileResponse(
        UUID id,
        String email,
        String status,
        String firstName,
        String lastName,
        String displayName,
        String bio,
        Map<String, String> socialLinks,
        String theme,
        String locale,
        String timezone,
        boolean emailNotifications,
        boolean pushNotifications,
        String originalAvatarUrl,
        String thumbnailAvatarUrl,
        Instant avatarUploadedAt,
        AdminMetadataResponse adminMetadata,
        Instant createdAt,
        Instant updatedAt
) {}