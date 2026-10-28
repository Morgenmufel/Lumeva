package lumeva.profileservice.domain.port.in;

import lumeva.profileservice.application.dto.ProvisionProfileCommand;
import lumeva.profileservice.domain.model.UserProfile;

import java.util.UUID;

public interface ProvisionProfileUseCase {

    /** Идемпотентное создание: если профиль уже есть, возвращает его. */
    UserProfile createInitialProfile(ProvisionProfileCommand command);

    /**
     * Профиль текущего пользователя; если его ещё нет — создаётся из данных токена.
     * Страховка на случай, если событие регистрации из Keycloak не дошло (ADR-008).
     */
    UserProfile getOrProvisionMyProfile(UUID userId, String username);
}
