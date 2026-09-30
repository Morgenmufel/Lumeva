package lumeva.profileservice.domain.model;

import java.time.Instant;

public record Avatar(
        String originalUrl,
        String thumbnailUrl,
        Instant uploadedAt
) {

    public static Avatar empty() {
        return new Avatar(null, null, null);
    }

    public boolean isEmpty() {
        return originalUrl == null && thumbnailUrl == null;
    }
}
