package lumeva.profileservice.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public record UpdateAvatarCommand(
        @JsonIgnore UUID userId,

        @NotBlank(message = "Original URL is required")
        String originalUrl,

        @NotBlank(message = "Thumbnail URL is required")
        String thumbnailUrl,

        Instant uploadedAt
) {
    public UpdateAvatarCommand withUserId(UUID userId) {
        return new UpdateAvatarCommand(
                userId,
                originalUrl,
                thumbnailUrl,
                uploadedAt != null ? uploadedAt : Instant.now()
        );
    }
}