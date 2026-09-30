package lumeva.profileservice.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record UpdatePersonalInfoCommand(
        @JsonIgnore UUID userId,

        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Size(max = 50, message = "Display name must not exceed 50 characters")
        String displayName,

        @Size(max = 500, message = "Bio must not exceed 500 characters")
        String bio,

        Map<String, String> socialLinks
) {
    public UpdatePersonalInfoCommand withUserId(UUID userId) {
        return new UpdatePersonalInfoCommand(userId, firstName, lastName, displayName, bio, socialLinks);
    }
}
