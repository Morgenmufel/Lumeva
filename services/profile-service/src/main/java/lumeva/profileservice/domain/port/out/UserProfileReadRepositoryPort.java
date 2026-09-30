package lumeva.profileservice.domain.port.out;

import lumeva.profileservice.domain.model.UserProfile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserProfileReadRepositoryPort {
    Optional<UserProfile> findPublicProfile(UUID subjectId);
    List<UserProfile> findProfilesBatch(Set<UUID> targetUserIds);
}
