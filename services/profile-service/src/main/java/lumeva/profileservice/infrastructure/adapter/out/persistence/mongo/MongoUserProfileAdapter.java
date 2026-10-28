package lumeva.profileservice.infrastructure.adapter.out.persistence.mongo;

import lombok.RequiredArgsConstructor;
import lumeva.profileservice.domain.model.ProfileStatus;
import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.domain.port.out.UserProfileReadRepositoryPort;
import lumeva.profileservice.infrastructure.adapter.out.persistence.mapper.UserProfileMongoMapper;
import lumeva.profileservice.infrastructure.adapter.out.persistence.repository.SpringDataMongoProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MongoUserProfileAdapter implements UserProfileReadRepositoryPort {

    private final SpringDataMongoProfileRepository repository;
    private final UserProfileMongoMapper mapper;

    @Override
    public Optional<UserProfile> findPublicProfile(UUID subjectId) {
        return repository.findById(subjectId)
                .filter(MongoUserProfileAdapter::isVisible)
                .map(mapper::toDomain);
    }

    @Override
    public List<UserProfile> findProfilesBatch(Set<UUID> targetUserIds) {
        return repository.findAllByIdIn(targetUserIds).stream()
                .filter(MongoUserProfileAdapter::isVisible)
                .map(mapper::toDomain)
                .toList();
    }

    private static final Set<String> KNOWN_STATUSES = Arrays.stream(ProfileStatus.values())
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    /**
     * Надгробия удалённых профилей наружу не отдаём. Статус, которого этот код ещё не знает
     * (новая версия producer'а), тоже пропускаем, а не падаем на его разборе.
     */
    private static boolean isVisible(UserProfileReadDocument document) {
        return document.getStatus() != null
                && !ProfileStatus.DELETED.name().equals(document.getStatus())
                && KNOWN_STATUSES.contains(document.getStatus());
    }
}
