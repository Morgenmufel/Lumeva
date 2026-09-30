package lumeva.profileservice.domain.model;

import java.util.Map;
import java.util.stream.Collectors;

public record PersonalInfo(String firstName,
                           String lastName,
                           String displayName,
                           String bio,
                           Map<String, String> socialLinks) {

    public PersonalInfo {
        if (bio != null && bio.length() > 500)
            throw new IllegalArgumentException("Bio length cannot be more than 500 symbols");
        socialLinks = socialLinks != null ? Map.copyOf(socialLinks) : Map.of();
    }

    public static PersonalInfo empty(){
        return new PersonalInfo(null, null, null,null, Map.of());
    }

    public PersonalInfo sanitizeBio() {
        return new PersonalInfo(this.firstName, this.lastName, this.displayName, null, this.socialLinks);
    }

    public PersonalInfo clear() {
        return new PersonalInfo(null, null, null, null, Map.of());
    }

}
