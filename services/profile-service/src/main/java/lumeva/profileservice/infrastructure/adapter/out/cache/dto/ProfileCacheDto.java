package lumeva.profileservice.infrastructure.adapter.out.cache.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCacheDto {
    private UUID id;
    private String email;
    private String status;
    private String firstName;
    private String lastName;
    private String displayName;
    private String bio;
    private Map<String, String> socialLinks;
    private String theme;
    private String locale;
    private String timezone;
    private boolean emailNotifications;
    private boolean pushNotifications;
    private String originalAvatarUrl;
    private String thumbnailAvatarUrl;
    private Instant avatarUploadedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
