package lumeva.profileservice.application.service;

import lumeva.profileservice.application.dto.*;
import lumeva.profileservice.application.event.ProfileCacheSyncEvent;
import lumeva.profileservice.domain.exceptions.ProfileNotFoundException;
import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.domain.port.in.SanitizeProfileUseCase;
import lumeva.profileservice.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProfileApplicationService implements SanitizeProfileUseCase {

    private final UserProfileWriteRepositoryPort profileRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AuditLogPort auditLog;
    private final ProfileEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public void sanitizeProfile(SanitizeProfileCommand command) {
        UserProfile profile = fetchProfileFromDb(command.targetUserId());

        profile.sanitize();
        UserProfile savedProfile = profileRepository.save(profile);

        applicationEventPublisher.publishEvent(new ProfileCacheSyncEvent(command.targetUserId()));
        auditLog.logAction(command.targetUserId(), command.moderatorId(), "MODERATOR", "SANITIZE_PROFILE", command.reason());
        eventPublisher.publishProfileUpdated(savedProfile);
    }

    private UserProfile fetchProfileFromDb(UUID userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId));
    }
}
