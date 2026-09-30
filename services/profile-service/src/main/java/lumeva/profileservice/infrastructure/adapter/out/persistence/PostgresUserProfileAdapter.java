package lumeva.profileservice.infrastructure.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lumeva.profileservice.domain.model.ProfileStatus;
import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.domain.port.out.UserProfileWriteRepositoryPort;
import lumeva.profileservice.infrastructure.adapter.out.persistence.mapper.UserProfilePersistenceMapper;
import lumeva.profileservice.infrastructure.adapter.out.persistence.repository.SpringDataUserProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostgresUserProfileAdapter implements UserProfileWriteRepositoryPort {

    private final SpringDataUserProfileRepository repository;
    private final UserProfilePersistenceMapper mapper;

    @Override
    public UserProfile save(UserProfile profile) {
        var entity = mapper.toEntity(profile);
        var savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<UserProfile> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }


    @Override
    public boolean existsById(UUID id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByEmailAndStatusNot(String email, ProfileStatus profileStatus) {
        return repository.existsByEmailAndStatusNot(email, profileStatus.toString());
    }
}