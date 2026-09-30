package lumeva.profileservice.domain.exceptions;

import lumeva.profileservice.domain.model.ProfileStatus;

import java.util.UUID;


public class ProfileBlockedException extends DomainException {
    public ProfileBlockedException(UUID userId, ProfileStatus status) {
        super(String.format("User profile %s is blocked with status %s", userId, status));
    }

    public ProfileBlockedException(String message) {
        super(message);
    }
}
