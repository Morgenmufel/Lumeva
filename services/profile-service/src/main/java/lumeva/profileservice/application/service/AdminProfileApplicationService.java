package lumeva.profileservice.application.service;



import lumeva.profileservice.application.dto.*;
import lumeva.profileservice.application.event.ProfileCacheSyncEvent;
import lumeva.profileservice.domain.exceptions.ProfileNotFoundException;
import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.domain.port.in.ModerateProfileUseCase;
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
public class AdminProfileApplicationService implements ModerateProfileUseCase {

    private final UserProfileWriteRepositoryPort profileRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AuditLogPort auditLog;
    private final ProfileEventPublisherPort eventPublisher;

    @Override
    @Transactional
    public void banUser(BanUserCommand command) {
        UserProfile profile = fetchProfileFromDb(command.targetUserId());

        profile.ban(command.adminId(), command.reason());
        profileRepository.save(profile);

        applicationEventPublisher.publishEvent(new ProfileCacheSyncEvent(command.targetUserId()));
        //profileCache.evict(command.targetUserId());
        auditLog.logAction(command.targetUserId(), command.adminId(), "ADMIN", "BAN_USER", command.reason());
        eventPublisher.publishProfileBanned(command.targetUserId(), command.reason());

        log.info("User {} was banned by admin {}", command.targetUserId(), command.adminId());
    }

    @Override
    @Transactional
    public void unbanUser(UnbanUserCommand command) {
        UserProfile profile = fetchProfileFromDb(command.targetUserId());

        profile.unban();
        profileRepository.save(profile);

        applicationEventPublisher.publishEvent(new ProfileCacheSyncEvent(command.targetUserId()));
        //profileCache.evict(command.targetUserId());
        auditLog.logAction(command.targetUserId(), command.adminId(), "ADMIN", "UNBAN_USER", command.reason());
        eventPublisher.publishProfileUnbanned(command.targetUserId());

        log.info("User {} was unbanned by admin {}", command.targetUserId(), command.adminId());
    }

    @Override
    @Transactional
    public void flagUser(FlagUserCommand command) {
        UserProfile profile = fetchProfileFromDb(command.targetUserId());

        profile.flag();
        profileRepository.save(profile);

        applicationEventPublisher.publishEvent(new ProfileCacheSyncEvent(command.targetUserId()));
        //profileCache.evict(command.targetUserId());
        auditLog.logAction(command.targetUserId(), command.moderatorId(), "MODERATOR", "FLAG_USER", command.reason());
    }

    @Override
    @Transactional
    public void suspendUser(SuspendUserCommand command) {
        UserProfile profile = fetchProfileFromDb(command.targetUserId());

        profile.suspend(command.suspendUntil());
        profileRepository.save(profile);

        applicationEventPublisher.publishEvent(new ProfileCacheSyncEvent(command.targetUserId()));
        //profileCache.evict(command.targetUserId());
        auditLog.logAction(command.targetUserId(), command.adminId(), "ADMIN", "SUSPEND_USER", command.reason());
    }

    @Override
    @Transactional
    public void sanitizeProfile(SanitizeProfileCommand command) {
        UserProfile profile = fetchProfileFromDb(command.targetUserId());

        profile.sanitize(command.moderatorId(), command.reason());
        UserProfile savedProfile = profileRepository.save(profile);

        applicationEventPublisher.publishEvent(new ProfileCacheSyncEvent(command.targetUserId()));
        //profileCache.evict(command.targetUserId());
        auditLog.logAction(command.targetUserId(), command.moderatorId(), "MODERATOR", "SANITIZE_PROFILE", command.reason());
        eventPublisher.publishProfileUpdated(savedProfile);
    }

    @Override
    @Transactional
    public void addModeratorNote(AddModeratorNoteCommand command) {
        UserProfile profile = fetchProfileFromDb(command.targetUserId());

        profile.addModeratorNote(command.moderatorId(), command.note());
        profileRepository.save(profile);

        auditLog.logAction(command.targetUserId(), command.moderatorId(), "MODERATOR", "ADD_NOTE", command.note());
    }

    private UserProfile fetchProfileFromDb(UUID userId) {
        return profileRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId));
    }
}