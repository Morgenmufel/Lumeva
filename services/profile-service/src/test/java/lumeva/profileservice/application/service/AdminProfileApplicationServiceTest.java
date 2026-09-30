package lumeva.profileservice.application.service;

import lumeva.profileservice.application.dto.BanUserCommand;
import lumeva.profileservice.application.event.ProfileCacheSyncEvent;
import lumeva.profileservice.domain.exceptions.ProfileNotFoundException;
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

import java.time.Instant;
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
    private UUID adminId;
    private UserProfile activeProfile;

    @BeforeEach
    void setUp() {
        targetUserId = UUID.randomUUID();
        adminId = UUID.randomUUID();
        activeProfile = UserProfile.createInitial(targetUserId, "user@lumeva.com");
    }

    @Test
    void banUser_HappyPath_UpdatesProfileStatusAndPublishesEvents() {
        BanUserCommand command = new BanUserCommand(targetUserId, adminId, "Spam policy violation")
                .withContext(targetUserId, adminId);
        when(profileRepository.findById(targetUserId)).thenReturn(Optional.of(activeProfile));

        adminService.banUser(command);

        assertThat(activeProfile.getStatus().name()).isEqualTo("BANNED");
        verify(profileRepository).save(activeProfile);
        verify(applicationEventPublisher).publishEvent(new ProfileCacheSyncEvent(targetUserId));
        verify(auditLog).logAction(targetUserId, adminId, "ADMIN", "BAN_USER", "Spam policy violation");
        verify(eventPublisher).publishProfileBanned(targetUserId, "Spam policy violation");
    }

    @Test
    void banUser_NegativeCase_ThrowsExceptionWhenProfileNotFound() {
        BanUserCommand command = new BanUserCommand(targetUserId, adminId, "Spam policy violation")
                .withContext(targetUserId, adminId);
        when(profileRepository.findById(targetUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.banUser(command))
                .isInstanceOf(ProfileNotFoundException.class);

        verify(profileRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }
}