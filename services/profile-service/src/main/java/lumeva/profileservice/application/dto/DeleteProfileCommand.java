package lumeva.profileservice.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

public record DeleteProfileCommand(
        @JsonIgnore UUID userId
) {
    public static DeleteProfileCommand of(UUID userId) {
        return new DeleteProfileCommand(userId);
    }
}
