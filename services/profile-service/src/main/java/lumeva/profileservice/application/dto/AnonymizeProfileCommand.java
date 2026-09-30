package lumeva.profileservice.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record AnonymizeProfileCommand(
        @JsonIgnore UUID userId,

        @NotBlank(message = "Reason for anonymization is required")
        String reason
) {
    public AnonymizeProfileCommand withUserId(UUID userId) {
        return new AnonymizeProfileCommand(userId, reason);
    }
}