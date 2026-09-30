package lumeva.profileservice.domain.port.out;

import lumeva.profileservice.domain.model.UserProfile;

import java.util.UUID;

public interface ProfileEventPublisherPort {
    void publishProfileCreated(UserProfile userProfile);
    void publishProfileUpdated(UserProfile profile);
    void publishProfileBanned(UUID userId, String reason);
    void publishProfileUnbanned(UUID userId);
    void publishProfileDeleted(UUID userId, String email);
}
