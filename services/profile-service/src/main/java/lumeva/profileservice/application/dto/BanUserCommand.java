package lumeva.profileservice.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record BanUserCommand(
        @JsonIgnore UUID targetUserId,
        @JsonIgnore UUID adminId,

        @NotBlank(message = "Ban reason is required")
        String reason
) {
    public BanUserCommand withContext(UUID targetUserId, UUID adminId) {
        return new BanUserCommand(targetUserId, adminId, reason);
    }
}