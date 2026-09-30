package lumeva.profileservice.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UnbanUserCommand(
        @JsonIgnore UUID targetUserId,
        @JsonIgnore UUID adminId,

        @NotBlank(message = "Reason for unbanning is required")
        String reason
) {
    public UnbanUserCommand withContext(UUID targetUserId, UUID adminId) {
        return new UnbanUserCommand(targetUserId, adminId, reason);
    }
}