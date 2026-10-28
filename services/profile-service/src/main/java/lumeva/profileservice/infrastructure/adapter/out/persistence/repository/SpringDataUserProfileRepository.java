package lumeva.profileservice.infrastructure.adapter.out.persistence.repository;

import lumeva.profileservice.infrastructure.adapter.out.persistence.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;
import java.util.List;

@Repository
public interface SpringDataUserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
    List<UserProfileEntity> findAllByIdIn(Set<UUID> ids);
}
