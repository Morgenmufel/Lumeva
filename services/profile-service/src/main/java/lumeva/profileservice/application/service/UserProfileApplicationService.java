package lumeva.profileservice.application.service;



import lumeva.profileservice.application.dto.*;
import lumeva.profileservice.application.event.ProfileCacheSyncEvent;
import lumeva.profileservice.domain.exceptions.DomainValidationException;
import lumeva.profileservice.domain.exceptions.ProfileNotFoundException;
import lumeva.profileservice.domain.model.Avatar;
import lumeva.profileservice.domain.model.PersonalInfo;
import lumeva.profileservice.domain.model.Preferences;
import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.domain.port.in.GetProfileUseCase;
import lumeva.profileservice.domain.port.in.ProvisionProfileUseCase;
import lumeva.profileservice.domain.port.in.UpdateProfileUseCase;
import lumeva.profileservice.domain.port.out.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileApplicationService implements GetProfileUseCase, UpdateProfileUseCase, ProvisionProfileUseCase {

    private final UserProfileWriteRepositoryPort writeRepository;
    private final UserProfileReadRepositoryPort readRepository;
    private final ProfileCachePort profileCache;
    private final ProfileEventPublisherPort eventPublisher;
    private final AuditLogPort auditLog;
    private final ApplicationEventPublisher applicationEventPublisher;


    @Override
    @Transactional(readOnly = true)
    public UserProfile getMyProfile(UUID userId) {
        return writeRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getPublicProfile(UUID subjectId) {
        return profileCache.get(subjectId)
                .orElseGet(() -> {
                    UserProfile profile = readRepository.findPublicProfile(subjectId)
                            .orElseThrow(() -> new ProfileNotFoundException(subjectId));
                    profileCache.put(profile);
                    return profile;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfile> getProfilesBatch(Set<UUID> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) return List.of();

        Map<UUID, UserProfile> cachedProfiles = profileCache.getAll(targetUserIds);
        Set<UUID> missingIds = new HashSet<>(targetUserIds);
        missingIds.removeAll(cachedProfiles.keySet());

        Map<UUID, UserProfile> resultMap = new HashMap<>(cachedProfiles);

        if (!missingIds.isEmpty()) {
            List<UserProfile> mongoProfiles = readRepository.findProfilesBatch(missingIds);
            Map<UUID, UserProfile> fetchedFromMongo = mongoProfiles.stream()
                    .collect(Collectors.toMap(UserProfile::getId, Function.identity()));

            resultMap.putAll(fetchedFromMongo);
            if (!fetchedFromMongo.isEmpty()) {
                profileCache.putAll(fetchedFromMongo);
            }
        }

        return List.copyOf(resultMap.values());
    }


    @Override
    @Transactional
    public UserProfile updatePersonalInfo(UpdatePersonalInfoCommand command) {
        UserProfile profile = fetchWriteProfile(command.userId());
        PersonalInfo personalInfo = new PersonalInfo(
                command.firstName(),
                command.lastName(),
                command.displayName(),
                command.bio(),
                command.socialLinks()
        );
        profile.updatePersonalInfo(personalInfo);
        return saveAndPublish(profile);
    }

    @Override
    @Transactional
    public UserProfile updatePreferences(UpdatePreferencesCommand command) {
        UserProfile profile = fetchWriteProfile(command.userId());
        Preferences preferences = new Preferences(
                command.theme(),
                command.locale(),
                command.timezone(),
                command.emailNotifications(),
                command.pushNotifications()
        );
        profile.updatePreferences(preferences);
        return saveAndPublish(profile);
    }

    @Override
    @Transactional
    public UserProfile updateAvatar(UpdateAvatarCommand command) {
        UserProfile profile = fetchWriteProfile(command.userId());
        Avatar avatar = new Avatar(command.originalUrl(), command.thumbnailUrl(), command.uploadedAt());
        profile.updateAvatar(avatar);
        return saveAndPublish(profile);
    }

    @Override
    @Transactional
    public UserProfile deleteAvatar(DeleteAvatarCommand command) {
        UserProfile profile = fetchWriteProfile(command.userId());
        profile.updateAvatar(Avatar.empty());
        return saveAndPublish(profile);
    }

    @Override
    @Transactional
    public UserProfile anonymizeProfile(AnonymizeProfileCommand command) {
        UserProfile profile = fetchWriteProfile(command.userId());
        profile.anonymize();
        UserProfile saved = saveAndPublish(profile);
        auditLog.logAction(command.userId(), null, "USER", "ANONYMIZE_PROFILE", command.reason());
        return saved;
    }

    @Override
    @Transactional
    public void deleteProfile(DeleteProfileCommand command) {
        UserProfile profile = fetchWriteProfile(command.userId());

        profile.deleteAccount();
        UserProfile savedProfile = writeRepository.save(profile);

        // в событие нужна новая версия агрегата, поэтому передаём сохранённый профиль
        eventPublisher.publishProfileDeleted(savedProfile);

        log.info("Soft-deleted user profile: {}", command.userId());
    }

    /**
     * Идемпотентное создание профиля: повтор события регистрации или JIT-вызов
     * для уже существующего профиля возвращает его, а не создаёт дубль (ADR-008).
     * Уникальность username гарантирует Keycloak, в БД дополнительно стоит unique-ограничение.
     */
    @Override
    @Transactional
    public UserProfile createInitialProfile(ProvisionProfileCommand command) {
        return writeRepository.findById(command.userId())
                .orElseGet(() -> {
                    try {
                        UserProfile savedProfile = writeRepository.save(
                                UserProfile.createInitial(command.userId(), command.username()));
                        eventPublisher.publishProfileCreated(savedProfile);
                        log.info("Provisioned new profile: {}", command.userId());
                        return savedProfile;
                    } catch (DataIntegrityViolationException e) {
                        log.warn("Username conflict while provisioning profile {}", command.userId());
                        throw new DomainValidationException("Username " + command.username() + " is already taken");
                    }
                });
    }

    @Override
    @Transactional
    public UserProfile getOrProvisionMyProfile(UUID userId, String username) {
        return writeRepository.findById(userId)
                .orElseGet(() -> {
                    String safeUsername = (username == null || username.isBlank())
                            ? "user-" + userId.toString().substring(0, 8)
                            : username;
                    log.info("Profile {} not found, provisioning just-in-time from token", userId);
                    return createInitialProfile(new ProvisionProfileCommand(userId, safeUsername));
                });
    }

    private UserProfile saveAndPublish(UserProfile profile) {
        UserProfile savedProfile = writeRepository.save(profile);
        eventPublisher.publishProfileUpdated(savedProfile);
        applicationEventPublisher.publishEvent(new ProfileCacheSyncEvent(profile.getId()));
        return savedProfile;
    }

    private UserProfile fetchWriteProfile(UUID userId) {
        return writeRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException(userId));
    }


}
