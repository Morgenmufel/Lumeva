package lumeva.profileservice.application.service;

import lumeva.profileservice.application.dto.SanitizeProfileCommand;
import lumeva.profileservice.application.event.ProfileCacheSyncEvent;
import lumeva.profileservice.domain.exceptions.ProfileNotFoundException;
import lumeva.profileservice.domain.model.PersonalInfo;
import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.domain.port.out.AuditLogPort;
import lumeva.profileservice.domain.port.out.ProfileEventPublisherPort;
import lumeva.profileservice.domain.port.out.UserProfileWriteRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminProfileApplicationServiceTest {

    @Mock private UserProfileWriteRepositoryPort profileRepository;
    @Mock private AuditLogPort auditLog;
    @Mock private ProfileEventPublisherPort eventPublisher;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks private AdminProfileApplicationService adminService;

    private UUID targetUserId;
    private UUID moderatorId;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        targetUserId = UUID.randomUUID();
        moderatorId = UUID.randomUUID();
        profile = UserProfile.createInitial(targetUserId, "target_user");
        profile.updatePersonalInfo(new PersonalInfo("John", "Doe", "JD", "Offensive bio", Map.of()));
    }

    @Test
    void sanitizeProfile_HappyPath_ClearsBioAndPublishesEvents() {
        var command = new SanitizeProfileCommand(targetUserId, moderatorId, "Offensive content");
        when(profileRepository.findById(targetUserId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(profile);

        adminService.sanitizeProfile(command);

        assertThat(profile.getPersonalInfo().bio()).isNull();
        assertThat(profile.getAvatar().isEmpty()).isTrue();
        verify(applicationEventPublisher).publishEvent(new ProfileCacheSyncEvent(targetUserId));
        verify(auditLog).logAction(targetUserId, moderatorId, "MODERATOR", "SANITIZE_PROFILE", "Offensive content");
        verify(eventPublisher).publishProfileUpdated(profile);
    }

    @Test
    void sanitizeProfile_ProfileNotFound_ThrowsAndSavesNothing() {
        var command = new SanitizeProfileCommand(targetUserId, moderatorId, "Offensive content");
        when(profileRepository.findById(targetUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.sanitizeProfile(command))
                .isInstanceOf(ProfileNotFoundException.class);

        verify(profileRepository, never()).save(any());
        verifyNoInteractions(eventPublisher, auditLog, applicationEventPublisher);
    }
}
