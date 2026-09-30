package lumeva.profileservice.domain.port.out;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lumeva.profileservice.domain.model.ProfileStatus;
import lumeva.profileservice.domain.model.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileWriteRepositoryPort {
    UserProfile save(UserProfile userProfile);
    Optional<UserProfile> findById(UUID userId);
    boolean existsById(UUID id);

    boolean existsByEmailAndStatusNot(@NotBlank(message = "Email is required")
                                      @Email(message = "Invalid email format") String email,
                                      ProfileStatus profileStatus);
}
