package lumeva.profileservice.infrastructure.adapter.out.persistence.mongo;

import lombok.RequiredArgsConstructor;
import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.domain.port.out.UserProfileReadRepositoryPort;
import lumeva.profileservice.infrastructure.adapter.out.persistence.mapper.UserProfileMongoMapper;
import lumeva.profileservice.infrastructure.adapter.out.persistence.repository.SpringDataMongoProfileRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MongoUserProfileAdapter implements UserProfileReadRepositoryPort {

    private final SpringDataMongoProfileRepository repository;
    private final UserProfileMongoMapper mapper;

    @Override
    public Optional<UserProfile> findPublicProfile(UUID subjectId) {
        return repository.findById(subjectId).map(mapper::toDomain);
    }

    @Override
    public List<UserProfile> findProfilesBatch(Set<UUID> targetUserIds) {
        return repository.findAllByIdIn(targetUserIds).stream()
                .map(mapper::toDomain)
                .toList();
    }
}