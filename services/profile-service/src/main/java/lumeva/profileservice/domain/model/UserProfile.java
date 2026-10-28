package lumeva.profileservice.domain.model;

import lumeva.profileservice.domain.exceptions.DomainValidationException;
import lumeva.profileservice.domain.exceptions.ProfileBlockedException;
import lumeva.profileservice.domain.exceptions.ProfileNotFoundException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class UserProfile {
    private final UUID id;
    /** Версия для оптимистической блокировки. null — профиль ещё не сохранён. */
    private final Long version;
    private String username;
    private PersonalInfo personalInfo;
    private Preferences preferences;
    private ProfileStatus status;
    private Avatar avatar;
    private final Instant createdAt;
    private Instant updatedAt;

    public UserProfile(
            UUID id,
            String username,
            PersonalInfo personalInfo,
            Preferences preferences,
            Avatar avatar,
            ProfileStatus status,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        this.version = version;
        this.id = Objects.requireNonNull(id, "User ID cannot be null");
        this.username = Objects.requireNonNull(username, "Username cannot be null");
        this.personalInfo = personalInfo != null ? personalInfo : PersonalInfo.empty();
        this.preferences = preferences != null ? preferences : Preferences.defaultPreferences();
        this.avatar = avatar != null ? avatar : Avatar.empty();
        this.status = status != null ? status : ProfileStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static UserProfile createInitial(UUID id, String username) {
        return new UserProfile(
                id,
                username,
                PersonalInfo.empty(),
                Preferences.defaultPreferences(),
                Avatar.empty(),
                ProfileStatus.ACTIVE,
                Instant.now(),
                Instant.now(),
                null
        );
    }

    public void updatePersonalInfo(PersonalInfo newInfo) {
        ensureEditable();
        this.personalInfo = Objects.requireNonNull(newInfo, "PersonalInfo cannot be null");
        touch();
    }

    public void updatePreferences(Preferences newPreferences) {
        ensureEditable();
        this.preferences = Objects.requireNonNull(newPreferences, "Preferences cannot be null");
        touch();
    }

    public void updateAvatar(Avatar newAvatar) {
        ensureEditable();
        this.avatar = Objects.requireNonNull(newAvatar, "Avatar cannot be null");
        touch();
    }

    /**
     * Принудительная очистка публичного контента по решению модерации.
     * Кто и почему её запросил, хранит Moderation.
     */
    public void sanitize() {
        this.personalInfo = this.personalInfo.sanitizeBio();
        this.avatar = Avatar.empty();
        touch();
    }

    /**
     * Анонимизация: убираем всё, что указывает на человека, включая публичный ник.
     * Профиль под санкцией анонимизировать нельзя, иначе это способ уйти от бана.
     */
    public void anonymize() {
        ensureEditable();
        this.username = "deleted-" + id.toString().substring(0, 8);
        this.personalInfo = PersonalInfo.empty();
        this.avatar = Avatar.empty();
        this.status = ProfileStatus.ANONYMIZED;
        touch();
    }

    /**
     * Переходы статусов:
     *   ACTIVE      -> DELETED      пользователь удаляет аккаунт
     *   ACTIVE      -> ANONYMIZED   пользователь запрашивает анонимизацию
     *   DELETED     -> ANONYMIZED   система по истечении срока хранения (позже)
     *   ACTIVE      -> RESTRICTED   событие санкции от Moderation
     *   RESTRICTED  -> ACTIVE       событие снятия санкции от Moderation
     * Любой другой переход запрещён.
     *
     * restrict() и liftRestriction() идемпотентны: Kafka может доставить событие повторно,
     * и повтор не должен падать или менять результат (ADR-007).
     */
    public void restrict() {
        if (this.status == ProfileStatus.RESTRICTED) {
            return;
        }
        if (this.status != ProfileStatus.ACTIVE) {
            throw new DomainValidationException("Cannot restrict profile in status " + this.status);
        }
        this.status = ProfileStatus.RESTRICTED;
        touch();
    }

    public void liftRestriction() {
        if (this.status == ProfileStatus.ACTIVE) {
            return;
        }
        if (this.status != ProfileStatus.RESTRICTED) {
            throw new DomainValidationException("Cannot lift restriction from profile in status " + this.status);
        }
        this.status = ProfileStatus.ACTIVE;
        touch();
    }

    public void deleteAccount() {
        if (this.status == ProfileStatus.RESTRICTED) {
            throw new ProfileBlockedException(this.id, this.status);
        }
        if (this.status == ProfileStatus.DELETED || this.status == ProfileStatus.ANONYMIZED) {
            throw new DomainValidationException("Profile is already deleted");
        }
        this.status = ProfileStatus.DELETED;
        touch();
    }

    public void ensureIsActive() {
        if (this.status == ProfileStatus.DELETED || this.status == ProfileStatus.ANONYMIZED) {
            throw new ProfileNotFoundException(this.id);
        }
        if (this.status == ProfileStatus.RESTRICTED) {
            throw new ProfileBlockedException(this.id, this.status);
        }
    }

    private void ensureEditable() {
        ensureIsActive();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getUsername() { return username; }
    public PersonalInfo getPersonalInfo() { return personalInfo; }
    public Preferences getPreferences() { return preferences; }
    public Avatar getAvatar() { return avatar; }
    public ProfileStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
