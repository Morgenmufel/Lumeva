package lumeva.profileservice.application.service;


import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.domain.port.out.ProfileCachePort;
import lumeva.profileservice.domain.port.out.UserProfileReadRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileApplicationServiceBatchTest {

    @Mock
    private UserProfileReadRepositoryPort readRepository;

    @Mock
    private ProfileCachePort profileCache;

    @InjectMocks
    private UserProfileApplicationService userService;

    @Test
    @DisplayName("getProfilesBatch должен корректно объединять результаты из Кэша и MongoDB")
    void getProfilesBatchHybridFetch() {
        UUID idCached = UUID.randomUUID();
        UUID idMongo = UUID.randomUUID();

        UserProfile cachedProfile = UserProfile.createInitial(idCached, "cached@lumeva.com");
        UserProfile mongoProfile = UserProfile.createInitial(idMongo, "mongo@lumeva.com");

        when(profileCache.getAll(Set.of(idCached, idMongo)))
                .thenReturn(Map.of(idCached, cachedProfile));

        when(readRepository.findProfilesBatch(Set.of(idMongo)))
                .thenReturn(List.of(mongoProfile));

        List<UserProfile> result = userService.getProfilesBatch(Set.of(idCached, idMongo));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserProfile::getId).containsExactlyInAnyOrder(idCached, idMongo);

        verify(profileCache).putAll(Map.of(idMongo, mongoProfile));
    }

    @Test
    @DisplayName("getProfilesBatch с пустым Set должен мгновенно возвращать пустой список без обращения к инфраструктуре")
    void getProfilesBatchEmptySet() {
        List<UserProfile> result = userService.getProfilesBatch(Set.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(profileCache, readRepository);
    }
}