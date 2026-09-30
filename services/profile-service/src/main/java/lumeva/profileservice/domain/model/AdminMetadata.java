package lumeva.profileservice.domain.model;


import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;

public record AdminMetadata(
        UUID bannedBy,
        String banReason,
        Instant bannedAt,
        List<String> moderatorNotes
) {

    public AdminMetadata {
        moderatorNotes = moderatorNotes != null ? List.copyOf(moderatorNotes) : List.of();
    }

    public static AdminMetadata empty() {
        return new AdminMetadata(null, null, null, List.of());
    }

    public AdminMetadata withBan(UUID adminId, String reason) {
        return new AdminMetadata(adminId, reason, Instant.now(), this.moderatorNotes);
    }

    public AdminMetadata unBan() {
        return new AdminMetadata(null, null, null, this.moderatorNotes);
    }

    public AdminMetadata addNote(String note) {
        List<String> updatedNotes = new ArrayList<>(this.moderatorNotes);
        updatedNotes.add(note);
        return new AdminMetadata(this.bannedBy, this.banReason, this.bannedAt, updatedNotes);
    }

}
