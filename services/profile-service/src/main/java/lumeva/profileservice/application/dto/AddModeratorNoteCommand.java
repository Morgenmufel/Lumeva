package lumeva.profileservice.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddModeratorNoteCommand(
        @JsonIgnore UUID targetUserId,
        @JsonIgnore UUID moderatorId,

        @NotBlank(message = "Note content cannot be empty")
        @Size(max = 1000, message = "Note cannot exceed 1000 characters")
        String note
) {
    public AddModeratorNoteCommand withContext(UUID targetUserId, UUID moderatorId) {
        return new AddModeratorNoteCommand(targetUserId, moderatorId, note);
    }
}
