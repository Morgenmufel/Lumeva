package lumeva.profileservice.domain.model;

import lumeva.profileservice.domain.exceptions.DomainValidationException;
import lumeva.profileservice.domain.exceptions.InvalidProfileDataException;
import lumeva.profileservice.domain.exceptions.ProfileBlockedException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserProfileDomainTest {

    private static UserProfile profileWithStatus(ProfileStatus status) {
        return new UserProfile(UUID.randomUUID(), "user", null, null, null, status, Instant.now(), Instant.now(), 0L);
    }

    @Test
    void createPersonalInfo_WithNullKeysAndValues_FiltersOutNulls() {
        Map<String, String> rawLinks = new HashMap<>();
        rawLinks.put("github", "url1");
        rawLinks.put(null, "url2");
        rawLinks.put("twitter", null);

        PersonalInfo info = new PersonalInfo("John", "Doe", "JD", "Bio", rawLinks);

        assertThat(info.socialLinks())
                .hasSize(1)
                .containsEntry("github", "url1")
                .doesNotContainKey(null);
    }

    @Test
    void defaultPreferences_HaveLocaleAndTimezoneInRightPlaces() {
        Preferences preferences = Preferences.defaultPreferences();

        assertThat(preferences.locale()).isEqualTo("ru");
        assertThat(preferences.timezone()).isEqualTo("UTC");
    }

    @Test
    void deleteAccount_WhenActive_ChangesStatusToDeleted() {
        UserProfile profile = UserProfile.createInitial(UUID.randomUUID(), "active_user");

        profile.deleteAccount();

        assertThat(profile.getStatus()).isEqualTo(ProfileStatus.DELETED);
    }

    @Test
    void deleteAccount_WhenRestricted_ThrowsProfileBlockedException() {
        UserProfile profile = profileWithStatus(ProfileStatus.RESTRICTED);

        assertThatThrownBy(profile::deleteAccount).isInstanceOf(ProfileBlockedException.class);
    }

    @Test
    void deleteAccount_WhenAlreadyDeleted_ThrowsDomainValidationException() {
        UserProfile profile = profileWithStatus(ProfileStatus.DELETED);

        assertThatThrownBy(profile::deleteAccount).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void updatePersonalInfo_WhenRestricted_ThrowsProfileBlockedException() {
        UserProfile profile = profileWithStatus(ProfileStatus.RESTRICTED);

        assertThatThrownBy(() -> profile.updatePersonalInfo(PersonalInfo.empty()))
                .isInstanceOf(ProfileBlockedException.class);
    }

    @Test
    void anonymize_ReplacesUsernameAndClearsPersonalData() {
        UserProfile profile = UserProfile.createInitial(UUID.randomUUID(), "real_nickname");
        profile.updatePersonalInfo(new PersonalInfo("John", "Doe", "JD", "Bio", Map.of("github", "url")));

        profile.anonymize();

        assertThat(profile.getStatus()).isEqualTo(ProfileStatus.ANONYMIZED);
        assertThat(profile.getUsername()).startsWith("deleted-").isNotEqualTo("real_nickname");
        assertThat(profile.getPersonalInfo()).isEqualTo(PersonalInfo.empty());
    }

    @Test
    void anonymize_WhenRestricted_ThrowsProfileBlockedException() {
        UserProfile profile = profileWithStatus(ProfileStatus.RESTRICTED);

        assertThatThrownBy(profile::anonymize).isInstanceOf(ProfileBlockedException.class);
    }

    @Test
    void restrict_FromActive_ChangesStatusToRestricted() {
        UserProfile profile = profileWithStatus(ProfileStatus.ACTIVE);

        profile.restrict();

        assertThat(profile.getStatus()).isEqualTo(ProfileStatus.RESTRICTED);
    }

    @Test
    void restrict_IsIdempotent() {
        UserProfile profile = profileWithStatus(ProfileStatus.RESTRICTED);

        profile.restrict();

        assertThat(profile.getStatus()).isEqualTo(ProfileStatus.RESTRICTED);
    }

    @Test
    void restrict_DeletedProfile_ThrowsDomainValidationException() {
        UserProfile profile = profileWithStatus(ProfileStatus.DELETED);

        assertThatThrownBy(profile::restrict).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void liftRestriction_FromRestricted_ChangesStatusToActive() {
        UserProfile profile = profileWithStatus(ProfileStatus.RESTRICTED);

        profile.liftRestriction();

        assertThat(profile.getStatus()).isEqualTo(ProfileStatus.ACTIVE);
    }

    @Test
    void liftRestriction_IsIdempotent() {
        UserProfile profile = profileWithStatus(ProfileStatus.ACTIVE);

        profile.liftRestriction();

        assertThat(profile.getStatus()).isEqualTo(ProfileStatus.ACTIVE);
    }

    @Test
    void liftRestriction_AnonymizedProfile_ThrowsDomainValidationException() {
        UserProfile profile = profileWithStatus(ProfileStatus.ANONYMIZED);

        assertThatThrownBy(profile::liftRestriction).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void personalInfo_BioLongerThan500_ThrowsInvalidProfileDataException() {
        assertThatThrownBy(() -> new PersonalInfo(null, null, null, "a".repeat(501), Map.of()))
                .isInstanceOf(InvalidProfileDataException.class);
    }
}
