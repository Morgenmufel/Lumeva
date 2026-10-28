package lumeva.profileservice.infrastructure.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
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
        // saveAndFlush, а не save: @Version увеличивается при flush. После простого save
        // в профиле осталась бы старая версия, и событие ушло бы с неверной aggregateVersion —
        // проекция отбросила бы его как устаревшее. Заодно конфликт версий (409) всплывает здесь.
        var savedEntity = repository.saveAndFlush(entity);
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
}
