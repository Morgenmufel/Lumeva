package lumeva.profileservice.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record SanitizeProfileCommand(
        @JsonIgnore UUID targetUserId,
        @JsonIgnore UUID moderatorId,

        @NotBlank(message = "Reason for sanitizing is required")
        String reason
) {
    public SanitizeProfileCommand withContext(UUID targetUserId, UUID moderatorId) {
        return new SanitizeProfileCommand(targetUserId, moderatorId, reason);
    }
}