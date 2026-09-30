package lumeva.profileservice.domain.port.in;

import lumeva.profileservice.application.dto.*;

import java.util.UUID;

public interface ModerateProfileUseCase {
    void banUser(BanUserCommand command);
    void unbanUser(UnbanUserCommand command);
    void flagUser(FlagUserCommand command);
    void suspendUser(SuspendUserCommand command);
    void sanitizeProfile(SanitizeProfileCommand command);
    void addModeratorNote(AddModeratorNoteCommand command);
}
