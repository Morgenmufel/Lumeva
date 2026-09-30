package lumeva.profileservice.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UpdatePreferencesCommand(
        @JsonIgnore UUID userId,

        @NotBlank(message = "Theme is required")
        String theme,

        @NotBlank(message = "Locale is required")
        String locale,

        @NotBlank(message = "Timezone is required")
        String timezone,

        boolean emailNotifications,
        boolean pushNotifications
) {
    public UpdatePreferencesCommand withUserId(UUID userId) {
        return new UpdatePreferencesCommand(userId, theme, locale, timezone, emailNotifications, pushNotifications);
    }
}