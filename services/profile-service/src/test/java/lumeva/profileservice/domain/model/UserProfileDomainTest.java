package lumeva.profileservice.domain.model;

import lumeva.profileservice.domain.exceptions.ProfileBlockedException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserProfileDomainTest {

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
    void deleteAccount_WhenStatusIsBanned_ThrowsProfileBlockedException() {
        UserProfile profile = UserProfile.createInitial(UUID.randomUUID(), "test@lumeva.com");
        profile.ban(UUID.randomUUID(), "Violation of terms");

        assertThatThrownBy(profile::deleteAccount)
                .isInstanceOf(ProfileBlockedException.class)
                .hasMessageContaining("is blocked with status BANNED");
    }

    @Test
    void deleteAccount_WhenStatusIsActive_ChangesStatusToDeleted() {
        UserProfile profile = UserProfile.createInitial(UUID.randomUUID(), "active@lumeva.com");

        profile.deleteAccount();

        assertThat(profile.getStatus()).isEqualTo(ProfileStatus.DELETED);
    }
}