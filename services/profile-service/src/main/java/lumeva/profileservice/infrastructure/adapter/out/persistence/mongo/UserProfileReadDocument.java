package lumeva.profileservice.infrastructure.adapter.out.persistence.mongo;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Document(collection = "user_profiles_read")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileReadDocument {

    @Id
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