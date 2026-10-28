package lumeva.profileservice.domain.port.out;

import lumeva.profileservice.domain.model.UserProfile;


public interface ProfileEventPublisherPort {
    void publishProfileCreated(UserProfile profile);
    void publishProfileUpdated(UserProfile profile);
    void publishProfileDeleted(UserProfile profile);
}
