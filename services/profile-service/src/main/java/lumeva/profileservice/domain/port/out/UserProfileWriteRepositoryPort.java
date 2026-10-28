package lumeva.profileservice.domain.port.out;

import lumeva.profileservice.domain.model.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileWriteRepositoryPort {
    UserProfile save(UserProfile userProfile);
    Optional<UserProfile> findById(UUID userId);
    boolean existsById(UUID id);
}
