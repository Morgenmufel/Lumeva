package lumeva.profileservice.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record SuspendUserCommand(
        @JsonIgnore UUID targetUserId,
        @JsonIgnore UUID adminId,

        @NotBlank(message = "Reason for suspension is required")
        String reason,

        @NotNull(message = "Suspend until date is required")
        @Future(message = "Suspend until timestamp must be in the future")
        Instant suspendUntil
) {
    public SuspendUserCommand withContext(UUID targetUserId, UUID adminId) {
        return new SuspendUserCommand(targetUserId, adminId, reason, suspendUntil);
    }
}