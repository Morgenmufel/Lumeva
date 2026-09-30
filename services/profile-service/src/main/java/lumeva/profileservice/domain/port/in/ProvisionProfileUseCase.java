package lumeva.profileservice.domain.port.in;

import lumeva.profileservice.application.dto.ProvisionProfileCommand;
import lumeva.profileservice.domain.model.UserProfile;
import java.util.UUID;

public interface ProvisionProfileUseCase {
    UserProfile createInitialProfile(ProvisionProfileCommand command);
}
