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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileApplicationServiceTest {

    @Mock private UserProfileWriteRepositoryPort writeRepository;
    @Mock private UserProfileReadRepositoryPort readRepository;
    @Mock private ProfileCachePort profileCache;
    @Mock private ProfileEventPublisherPort eventPublisher;
    @Mock private AuditLogPort auditLog;

    @InjectMocks private UserProfileApplicationService userService;

    @Test
    @DisplayName("createInitialProfile создаёт профиль и публикует событие")
    void createInitialProfileSuccess() {
        UUID userId = UUID.randomUUID();
        when(writeRepository.findById(userId)).thenReturn(Optional.empty());
        when(writeRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile created = userService.createInitialProfile(new ProvisionProfileCommand(userId, "new_user"));

        assertThat(created.getId()).isEqualTo(userId);
        assertThat(created.getUsername()).isEqualTo("new_user");
        verify(eventPublisher).publishProfileCreated(created);
    }

    @Test
    @DisplayName("createInitialProfile идемпотентен: повтор возвращает существующий профиль без дубля и события")
    void createInitialProfileIsIdempotent() {
        UUID userId = UUID.randomUUID();
        UserProfile existing = UserProfile.createInitial(userId, "existing_user");
        when(writeRepository.findById(userId)).thenReturn(Optional.of(existing));

        UserProfile result = userService.createInitialProfile(new ProvisionProfileCommand(userId, "existing_user"));

        assertThat(result).isSameAs(existing);
        verify(writeRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("createInitialProfile с занятым username бросает DomainValidationException")
    void createInitialProfileUsernameConflict() {
        UUID userId = UUID.randomUUID();
        when(writeRepository.findById(userId)).thenReturn(Optional.empty());
        when(writeRepository.save(any(UserProfile.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> userService.createInitialProfile(new ProvisionProfileCommand(userId, "taken")))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("taken");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("deleteProfile мягко удаляет профиль и публикует событие без PII")
    void deleteProfileSuccess() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.createInitial(userId, "to_delete");
        when(writeRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(writeRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        userService.deleteProfile(DeleteProfileCommand.of(userId));

        verify(writeRepository).save(argThat(saved -> saved.getStatus() == ProfileStatus.DELETED));
        verify(eventPublisher).publishProfileDeleted(argThat(deleted -> deleted.getId().equals(userId)));
    }

    @Test
    @DisplayName("getPublicProfile возвращает профиль из кэша, если он там есть")
    void getPublicProfileCacheHit() {
        UUID userId = UUID.randomUUID();
        UserProfile cachedProfile = UserProfile.createInitial(userId, "cached_user");
        when(profileCache.get(userId)).thenReturn(Optional.of(cachedProfile));

        UserProfile result = userService.getPublicProfile(userId);

        assertThat(result.getId()).isEqualTo(userId);
        verify(readRepository, never()).findPublicProfile(any());
    }

    @Test
    @DisplayName("getOrProvisionMyProfile создаёт профиль из токена, если его ещё нет (JIT)")
    void getOrProvisionCreatesMissingProfile() {
        UUID userId = UUID.randomUUID();
        when(writeRepository.findById(userId)).thenReturn(Optional.empty());
        when(writeRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile result = userService.getOrProvisionMyProfile(userId, "jit_user");

        assertThat(result.getUsername()).isEqualTo("jit_user");
        verify(eventPublisher).publishProfileCreated(result);
    }

    @Test
    @DisplayName("getOrProvisionMyProfile без preferred_username подставляет user-<id>")
    void getOrProvisionUsesFallbackUsername() {
        UUID userId = UUID.randomUUID();
        when(writeRepository.findById(userId)).thenReturn(Optional.empty());
        when(writeRepository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile result = userService.getOrProvisionMyProfile(userId, " ");

        assertThat(result.getUsername()).isEqualTo("user-" + userId.toString().substring(0, 8));
    }

    @Test
    @DisplayName("getOrProvisionMyProfile возвращает существующий профиль без записи")
    void getOrProvisionReturnsExistingProfile() {
        UUID userId = UUID.randomUUID();
        UserProfile existing = UserProfile.createInitial(userId, "existing");
        when(writeRepository.findById(userId)).thenReturn(Optional.of(existing));

        assertThat(userService.getOrProvisionMyProfile(userId, "existing")).isSameAs(existing);
        verify(writeRepository, never()).save(any());
    }
}
