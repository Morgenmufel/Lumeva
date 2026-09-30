package lumeva.profileservice.infrastructure.adapter.out.persistence.repository;

import lumeva.profileservice.infrastructure.adapter.out.persistence.mongo.UserProfileReadDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;
import java.util.List;

@Repository
public interface SpringDataMongoProfileRepository extends JpaRepository<UserProfileReadDocument, UUID> {
    List<UserProfileReadDocument> findAllByIdIn(Set<UUID> ids);
}
