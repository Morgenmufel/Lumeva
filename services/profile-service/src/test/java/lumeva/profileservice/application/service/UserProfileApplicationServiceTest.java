package lumeva.profileservice.application.service;

import lumeva.profileservice.application.dto.DeleteProfileCommand;
import lumeva.profileservice.application.dto.ProvisionProfileCommand;
import lumeva.profileservice.domain.exceptions.DomainValidationException;
import lumeva.profileservice.domain.model.ProfileStatus;
import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.domain.port.out.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileApplicationServiceTest {

    @Mock
    private UserProfileWriteRepositoryPort writeRepository;

    @Mock
    private UserProfileReadRepositoryPort readRepository;

    @Mock
    private ProfileCachePort profileCache;

    @Mock
    private ProfileEventPublisherPort eventPublisher;

    @Mock
    private AuditLogPort auditLog;

    @InjectMocks
    private UserProfileApplicationService userService;

    @Test
    @DisplayName("createInitialProfile должен успешно создавать профиль и публиковать событие")
    void createInitialProfileSuccess() {
        UUID userId = UUID.randomUUID();
        String email = "newuser@lumeva.com";
        ProvisionProfileCommand command = new ProvisionProfileCommand(userId, email);

        when(writeRepository.existsByEmailAndStatusNot(email, ProfileStatus.DELETED)).thenReturn(false);
        when(writeRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile created = userService.createInitialProfile(command);

        assertThat(created.getId()).isEqualTo(userId);
        assertThat(created.getEmail()).isEqualTo(email);
        verify(eventPublisher).publishProfileCreated(any(UserProfile.class));
    }

    @Test
    @DisplayName("createInitialProfile должен бросать ошибку, если активный профиль с таким email уже существует")
    void createInitialProfileDuplicateEmail() {
        String email = "existing@lumeva.com";
        ProvisionProfileCommand command = new ProvisionProfileCommand(UUID.randomUUID(), email);

        when(writeRepository.existsByEmailAndStatusNot(email, ProfileStatus.DELETED)).thenReturn(true);

        assertThatThrownBy(() -> userService.createInitialProfile(command))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("already exists");

        verify(writeRepository, never()).save(any());
        verify(eventPublisher, never()).publishProfileCreated(any());
    }

    @Test
    @DisplayName("deleteProfile должен мягко удалять профиль и отправлять событие с его email")
    void deleteProfileSuccess() {
        UUID userId = UUID.randomUUID();
        String email = "delete@lumeva.com";
        UserProfile profile = UserProfile.createInitial(userId, email);

        when(writeRepository.findById(userId)).thenReturn(Optional.of(profile));

        userService.deleteProfile(DeleteProfileCommand.of(userId));

        verify(writeRepository).save(argThat(saved -> saved.getStatus() == ProfileStatus.DELETED));
        verify(eventPublisher).publishProfileDeleted(eq(userId), eq(email));
    }

    @Test
    @DisplayName("getPublicProfile должен возвращать профиль из кэша, если он там есть")
    void getPublicProfileCacheHit() {
        UUID userId = UUID.randomUUID();
        UserProfile cachedProfile = UserProfile.createInitial(userId, "cache@lumeva.com");

        when(profileCache.get(userId)).thenReturn(Optional.of(cachedProfile));

        UserProfile result = userService.getPublicProfile(userId);

        assertThat(result.getId()).isEqualTo(userId);
        verify(readRepository, never()).findPublicProfile(any());
    }
}