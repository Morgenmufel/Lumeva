package lumeva.profileservice.domain.port.in;

import lumeva.profileservice.application.dto.*;
import lumeva.profileservice.domain.model.UserProfile;
import java.util.UUID;

public interface UpdateProfileUseCase {
    UserProfile updatePersonalInfo(UpdatePersonalInfoCommand command);
    UserProfile updatePreferences(UpdatePreferencesCommand command);
    UserProfile updateAvatar(UpdateAvatarCommand command);
    UserProfile deleteAvatar(DeleteAvatarCommand command);
    UserProfile anonymizeProfile(AnonymizeProfileCommand command);
    void deleteProfile(DeleteProfileCommand command);
}
