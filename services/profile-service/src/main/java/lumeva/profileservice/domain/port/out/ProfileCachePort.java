package lumeva.profileservice.domain.port.out;

import lumeva.profileservice.domain.model.UserProfile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;

public interface ProfileCachePort {
    Optional<UserProfile> get(UUID userId);
    void put(UserProfile profile);
    void evict(UUID id);

    Map<UUID, UserProfile> getAll(Set<UUID> ids);
    void putAll(Map<UUID, UserProfile> profiles);
}
