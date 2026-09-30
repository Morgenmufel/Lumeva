package lumeva.profileservice.infrastructure.adapter.in.rest.dto;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

public record AdminMetadataResponse(
        UUID bannedBy,
        String banReason,
        Instant bannedAt,
        Instant suspendedUntil,
        List<String> moderatorNotes
) {}
