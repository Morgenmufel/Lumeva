package lumeva.profileservice.domain.model;

import lumeva.profileservice.domain.exceptions.DomainValidationException;
import lumeva.profileservice.domain.exceptions.ProfileBlockedException;
import lumeva.profileservice.domain.exceptions.ProfileNotFoundException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class UserProfile {
    private final UUID id;
    private String email;
    private PersonalInfo personalInfo;
    private Preferences preferences;
    private ProfileStatus profileStatus;
    private Avatar avatar;
    private AdminMetadata adminMetadata;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant suspendedUntil;

    public UserProfile(
            UUID id,
            String email,
            PersonalInfo personalInfo,
            Preferences preferences,
            Avatar avatar,
            AdminMetadata adminMetadata,
            ProfileStatus profileStatus,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "User ID cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.personalInfo = personalInfo != null ? personalInfo : PersonalInfo.empty();
        this.preferences = preferences != null ? preferences : Preferences.defaultPreferences();
        this.avatar = avatar != null ? avatar : Avatar.empty();
        this.adminMetadata = adminMetadata != null ? adminMetadata : AdminMetadata.empty();
        this.profileStatus = profileStatus != null ? profileStatus : ProfileStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static UserProfile createInitial(UUID id, String email) {
        return new UserProfile(
                id,
                email,
                PersonalInfo.empty(),
                Preferences.defaultPreferences(),
                Avatar.empty(),
                AdminMetadata.empty(),
                ProfileStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
    }


    public void updatePersonalInfo(PersonalInfo newInfo) {
        ensureNotBlocked();
        this.personalInfo = Objects.requireNonNull(newInfo, "PersonalInfo cannot be null");
        this.updatedAt = Instant.now();
    }

    public void updatePreferences(Preferences newPreferences) {
        ensureNotBlocked();
        this.preferences = Objects.requireNonNull(newPreferences, "Preferences cannot be null");
        this.updatedAt = Instant.now();
    }

    public void updateAvatar(Avatar newAvatar) {
        ensureNotBlocked();
        this.avatar = Objects.requireNonNull(newAvatar, "Avatar cannot be null");
        this.updatedAt = Instant.now();
    }

    public void ban(UUID adminId, String reason) {
        this.profileStatus = ProfileStatus.BANNED;
        this.adminMetadata = this.adminMetadata.withBan(adminId, reason);
        this.updatedAt = Instant.now();
    }

    public void unban() {
        this.profileStatus = ProfileStatus.ACTIVE;
        this.adminMetadata = this.adminMetadata.unBan();
        this.updatedAt = Instant.now();
    }

    public void flag() {
        if (this.profileStatus == ProfileStatus.ACTIVE) {
            this.profileStatus = ProfileStatus.FLAGGED;
            this.updatedAt = Instant.now();
        }
    }

    public void suspend(Instant suspendedUntil) {
        ensureNotBlocked();
        this.suspendedUntil = suspendedUntil;
        this.profileStatus = ProfileStatus.FROZEN;
        this.updatedAt = Instant.now();
    }

    public void sanitize(UUID moderatorId, String reason) {
        this.personalInfo = this.personalInfo.sanitizeBio();
        this.avatar = Avatar.empty();
        this.adminMetadata = this.adminMetadata.addNote("Sanitized at " + Instant.now()
                + " by " + moderatorId + ": " + reason);
        this.updatedAt = Instant.now();
    }

    public void addModeratorNote(UUID moderatorId, String note) {
        this.adminMetadata = this.adminMetadata.addNote("Note at " + Instant.now()
                + " by " + moderatorId + ": " + note);
        this.updatedAt = Instant.now();
    }

    public void anonymize() {
        this.email = "anonymized_" + id + "@deleted.lumeva.com";
        this.personalInfo = PersonalInfo.empty();
        this.avatar = Avatar.empty();
        this.profileStatus = ProfileStatus.ANONYMIZED;
        this.updatedAt = Instant.now();
    }

    private void ensureNotBlocked() {
        if (this.profileStatus == ProfileStatus.BANNED || this.profileStatus == ProfileStatus.FROZEN) {
            throw new IllegalStateException("Operation denied. Profile status is " + this.profileStatus);
        }
    }

    public void deleteAccount() {
        if (this.profileStatus == ProfileStatus.BANNED) {
            throw new ProfileBlockedException(this.id, this.profileStatus);
        }
        if (this.profileStatus == ProfileStatus.DELETED) {
            throw new DomainValidationException("Profile is already deleted");
        }

        this.profileStatus = ProfileStatus.DELETED;
        this.updatedAt = Instant.now();
    }

    public void ensureIsActive() {
        if (this.profileStatus == ProfileStatus.DELETED) {
            throw new ProfileNotFoundException(this.id);
        }
        if (this.profileStatus == ProfileStatus.BANNED) {
            throw new ProfileBlockedException(this.id, this.profileStatus);
        }
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public PersonalInfo getPersonalInfo() { return personalInfo; }
    public Preferences getPreferences() { return preferences; }
    public Avatar getAvatar() { return avatar; }
    public AdminMetadata getAdminMetadata() { return adminMetadata; }
    public ProfileStatus getStatus() { return profileStatus; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getSuspendedUntil() { return suspendedUntil; }
}
