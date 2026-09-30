package lumeva.profileservice.infrastructure.adapter.in.rest.dto;

import java.util.Map;
import java.util.UUID;

public record PublicProfileResponse(
        UUID id,
        String displayName,
        String firstName,
        String lastName,
        String bio,
        Map<String, String> socialLinks,
        String thumbnailAvatarUrl,
        String status
) {}