package lumeva.profileservice.infrastructure.adapter.out.persistence;

import lumeva.profileservice.domain.model.ProfileStatus;
import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.infrastructure.adapter.out.persistence.entity.UserProfileEntity;
import lumeva.profileservice.infrastructure.adapter.out.persistence.mapper.UserProfilePersistenceMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Оптимистическая блокировка работает, только если версия проходит путь
 * сущность -> домен -> сущность без потерь. Если маппер её потеряет,
 * Hibernate будет считать каждое сохранение конфликтом или пропустит проверку.
 * Сам конфликт двух параллельных обновлений проверим в интеграционном тесте с Postgres.
 */
class UserProfilePersistenceMapperTest {

    private final UserProfilePersistenceMapper mapper = Mappers.getMapper(UserProfilePersistenceMapper.class);

    @Test
    void versionSurvivesRoundTrip() {
        UserProfileEntity entity = mapper.toEntity(new UserProfile(
                UUID.randomUUID(), "user", null, null, null, ProfileStatus.ACTIVE, Instant.now(), Instant.now(), 7L));
        entity.setVersion(7L);

        UserProfile domain = mapper.toDomain(entity);
        UserProfileEntity back = mapper.toEntity(domain);

        assertThat(domain.getVersion()).isEqualTo(7L);
        assertThat(back.getVersion()).isEqualTo(7L);
    }

    @Test
    void newProfileHasNullVersion() {
        UserProfileEntity entity = mapper.toEntity(UserProfile.createInitial(UUID.randomUUID(), "new_user"));

        assertThat(entity.getVersion()).isNull();
    }
}
