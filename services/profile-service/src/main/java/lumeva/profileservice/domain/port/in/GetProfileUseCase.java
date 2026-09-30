package lumeva.profileservice.domain.port.in;

import lumeva.profileservice.domain.model.UserProfile;

import java.util.UUID;
import java.util.List;
import java.util.Set;

public interface GetProfileUseCase {
    UserProfile getMyProfile(UUID userId);
    UserProfile getPublicProfile(UUID subjectId);
    List<UserProfile> getProfilesBatch(Set<UUID> targetUserIds);
}
