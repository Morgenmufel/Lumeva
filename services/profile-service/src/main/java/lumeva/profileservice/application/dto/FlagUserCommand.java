package lumeva.profileservice.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record FlagUserCommand(
        @JsonIgnore UUID targetUserId,
        @JsonIgnore UUID moderatorId,

        @NotBlank(message = "Reason for flagging is required")
        String reason
) {
    public FlagUserCommand withContext(UUID targetUserId, UUID moderatorId) {
        return new FlagUserCommand(targetUserId, moderatorId, reason);
    }
}