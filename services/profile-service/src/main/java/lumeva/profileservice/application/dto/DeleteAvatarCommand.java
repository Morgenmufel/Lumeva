package lumeva.profileservice.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

public record DeleteAvatarCommand(
        @JsonIgnore UUID userId
) {
    public static DeleteAvatarCommand of(UUID userId) {
        return new DeleteAvatarCommand(userId);
    }
}