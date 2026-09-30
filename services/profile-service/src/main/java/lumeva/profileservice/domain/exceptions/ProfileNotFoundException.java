package lumeva.profileservice.domain.exceptions;

import java.util.UUID;

public class ProfileNotFoundException extends DomainException {
  public ProfileNotFoundException(UUID userId) {
    super("User profile with ID " + userId + " was not found");
  }

  public ProfileNotFoundException(String message) {
    super(message);
  }
}
