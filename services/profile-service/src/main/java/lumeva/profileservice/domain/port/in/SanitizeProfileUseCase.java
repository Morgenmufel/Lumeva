package lumeva.profileservice.domain.port.in;

import lumeva.profileservice.application.dto.SanitizeProfileCommand;

public interface SanitizeProfileUseCase {
    void sanitizeProfile(SanitizeProfileCommand command);
}
